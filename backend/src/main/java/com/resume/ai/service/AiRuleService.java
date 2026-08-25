package com.resume.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.ai.dto.AiRuleRequest;
import com.resume.ai.dto.AiRuleResponse;
import com.resume.ai.dto.AiRuleStatsResponse;
import com.resume.ai.dto.AiChatRequest;
import com.resume.ai.entity.AiRule;
import com.resume.ai.mapper.AiRuleMapper;
import com.resume.ai.provider.LlmProvider;
import com.resume.ai.provider.ProviderRouter;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 规则管理业务服务。
 * <p>
 * 规则生命周期：draft（草稿）→ active（发布生效）→ disabled（停用），
 * 发布生成新版本行，历史版本保留用于回滚。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRuleService {

    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_DISABLED = "disabled";

    private static final String TEST_FEATURE_KEY = "resume-review";

    private final AiRuleMapper aiRuleMapper;
    private final ProviderRouter providerRouter;

    /**
     * 管理端分页查询。
     */
    public Page<AiRuleResponse> list(int page, int size, String keyword, String ruleType, String status) {
        LambdaQueryWrapper<AiRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiRule::getDeleted, BizConstant.NOT_DELETED);
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(AiRule::getName, kw)
                    .or().like(AiRule::getDescription, kw));
        }
        if (StringUtils.isNotBlank(ruleType)) {
            wrapper.eq(AiRule::getRuleType, ruleType);
        }
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(AiRule::getStatus, status);
        }
        wrapper.orderByDesc(AiRule::getUpdatedAt);

        Page<AiRule> pageParam = new Page<>(page, size);
        Page<AiRule> result = aiRuleMapper.selectPage(pageParam, wrapper);

        Page<AiRuleResponse> responsePage = new Page<>();
        responsePage.setRecords(result.getRecords().stream().map(AiRuleResponse::from).toList());
        responsePage.setTotal(result.getTotal());
        responsePage.setCurrent(result.getCurrent());
        responsePage.setSize(result.getSize());
        responsePage.setPages(result.getPages());
        return responsePage;
    }

    /**
     * 规则详情（含全部版本）。
     */
    public AiRuleResponse get(String id) {
        AiRule rule = getRule(id);
        return AiRuleResponse.from(rule);
    }

    /**
     * 管理端统计。
     */
    public AiRuleStatsResponse stats() {
        AiRuleStatsResponse response = new AiRuleStatsResponse();
        response.setTotal(countByStatus(null));
        response.setActive(countByStatus(STATUS_ACTIVE));
        response.setDisabled(countByStatus(STATUS_DISABLED));
        response.setDraft(countByStatus(STATUS_DRAFT));
        response.setTodayPublished(aiRuleMapper.selectCount(
                new LambdaQueryWrapper<AiRule>()
                        .eq(AiRule::getDeleted, BizConstant.NOT_DELETED)
                        .eq(AiRule::getStatus, STATUS_ACTIVE)
                        .ge(AiRule::getPublishedAt, LocalDateTime.now().toLocalDate().atStartOfDay())));
        return response;
    }

    /**
     * 创建规则（草稿）。
     */
    @Transactional(rollbackFor = Exception.class)
    public AiRuleResponse create(String operatorId, AiRuleRequest request) {
        validateType(request.getRuleType());
        AiRule rule = new AiRule();
        applyRequest(rule, request);
        rule.setStatus(STATUS_DRAFT);
        rule.setVersion(1);
        rule.setCreatedBy(operatorId);
        rule.setDeleted(BizConstant.NOT_DELETED);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        aiRuleMapper.insert(rule);
        // 首个版本族 ID 为自身 ID
        rule.setFamilyId(rule.getId());
        aiRuleMapper.updateById(rule);
        return AiRuleResponse.from(rule);
    }

    /**
     * 保存草稿（更新草稿或已停用规则的草稿内容；生效中规则更新走发布新版本）。
     */
    @Transactional(rollbackFor = Exception.class)
    public AiRuleResponse saveDraft(String operatorId, String id, AiRuleRequest request) {
        AiRule rule = getRule(id);
        if (STATUS_ACTIVE.equals(rule.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "生效中的规则请通过「发布新版本」更新。");
        }
        applyRequest(rule, request);
        rule.setUpdatedAt(LocalDateTime.now());
        aiRuleMapper.updateById(rule);
        return AiRuleResponse.from(rule);
    }

    /**
     * 发布新版本：原生效版本置为 disabled，新建 active 版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public AiRuleResponse publish(String operatorId, String id, AiRuleRequest request) {
        AiRule latest = getRule(id);
        String familyId = latest.getFamilyId() != null ? latest.getFamilyId() : latest.getId();
        // 将当前所有版本停用，保留历史
        List<AiRule> allVersions = listFamily(familyId);
        for (AiRule version : allVersions) {
            if (STATUS_ACTIVE.equals(version.getStatus())) {
                version.setStatus(STATUS_DISABLED);
                version.setUpdatedAt(LocalDateTime.now());
                aiRuleMapper.updateById(version);
            }
        }

        int nextVersion = allVersions.stream()
                .mapToInt(v -> v.getVersion() == null ? 1 : v.getVersion())
                .max().orElse(0) + 1;

        AiRule rule = new AiRule();
        if (request != null) {
            rule.setName(request.getName() != null ? request.getName() : latest.getName());
            rule.setRuleType(request.getRuleType() != null ? request.getRuleType() : latest.getRuleType());
            applyRequest(rule, request);
            // 发布新版本时 request 缺省的字段继承 latest 版本的值，避免把生效中的配置置 null
            if (rule.getDescription() == null) {
                rule.setDescription(latest.getDescription());
            }
            if (rule.getSystemPrompt() == null) {
                rule.setSystemPrompt(latest.getSystemPrompt());
            }
            if (rule.getUserPrompt() == null) {
                rule.setUserPrompt(latest.getUserPrompt());
            }
            if (rule.getParams() == null) {
                rule.setParams(latest.getParams());
            }
        } else {
            rule.setName(latest.getName());
            rule.setRuleType(latest.getRuleType());
            rule.setDescription(latest.getDescription());
            rule.setSystemPrompt(latest.getSystemPrompt());
            rule.setUserPrompt(latest.getUserPrompt());
            rule.setParams(latest.getParams());
        }
        rule.setFamilyId(familyId);
        rule.setStatus(STATUS_ACTIVE);
        rule.setVersion(nextVersion);
        rule.setPublishedAt(LocalDateTime.now());
        rule.setCreatedBy(operatorId);
        rule.setDeleted(BizConstant.NOT_DELETED);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        aiRuleMapper.insert(rule);
        log.info("ai rule published: name={}, version={}, operatorId={}", rule.getName(), nextVersion, operatorId);
        return AiRuleResponse.from(rule);
    }

    /**
     * 启停规则。
     */
    @Transactional(rollbackFor = Exception.class)
    public AiRuleResponse toggle(String operatorId, String id, String status) {
        if (!STATUS_ACTIVE.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "状态仅支持 active / disabled。");
        }
        AiRule rule = getRule(id);
        rule.setStatus(status);
        if (STATUS_ACTIVE.equals(status) && rule.getPublishedAt() == null) {
            rule.setPublishedAt(LocalDateTime.now());
        }
        rule.setUpdatedAt(LocalDateTime.now());
        aiRuleMapper.updateById(rule);
        return AiRuleResponse.from(rule);
    }

    /**
     * 版本列表（按版本号降序）。
     */
    public List<AiRuleResponse> versions(String id) {
        AiRule current = getRule(id);
        return listFamily(current.getFamilyId() != null ? current.getFamilyId() : current.getId())
                .stream().map(AiRuleResponse::from).toList();
    }

    /**
     * 回滚到指定版本：原版本新建为 active，其余停用。
     */
    @Transactional(rollbackFor = Exception.class)
    public AiRuleResponse rollback(String operatorId, String id, int version) {
        AiRule current = getRule(id);
        String familyId = current.getFamilyId() != null ? current.getFamilyId() : current.getId();
        List<AiRule> all = listFamily(familyId);
        AiRule target = all.stream()
                .filter(v -> v.getVersion() != null && v.getVersion() == version)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "目标版本不存在。"));

        for (AiRule versionRow : all) {
            if (STATUS_ACTIVE.equals(versionRow.getStatus())) {
                versionRow.setStatus(STATUS_DISABLED);
                versionRow.setUpdatedAt(LocalDateTime.now());
                aiRuleMapper.updateById(versionRow);
            }
        }
        target.setStatus(STATUS_ACTIVE);
        target.setPublishedAt(LocalDateTime.now());
        target.setUpdatedAt(LocalDateTime.now());
        aiRuleMapper.updateById(target);
        log.info("ai rule rolled back: name={}, version={}, operatorId={}", target.getName(), version, operatorId);
        return AiRuleResponse.from(target);
    }

    /**
     * 删除规则（逻辑删除；生效中规则不可删）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        AiRule rule = getRule(id);
        if (STATUS_ACTIVE.equals(rule.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "生效中的规则不可删除，请先停用。");
        }
        // @TableLogic 下 deleteById 自动转为 UPDATE deleted=1；
        // setDeleted+updateById 会把逻辑删除字段排除在 SET 外导致静默失效，禁止使用。
        // 规则当前无内存/Redis 缓存，无需额外失效处理
        aiRuleMapper.deleteById(id);
    }

    /**
     * 测试运行：调用已配置的 LLM（未配置时返回占位结果）。
     */
    public Map<String, Object> testRun(String id, String sampleInput) {
        AiRule rule = getRule(id);
        if (StringUtils.isBlank(rule.getSystemPrompt()) && StringUtils.isBlank(rule.getUserPrompt())) {
            return Map.of("ok", true, "output", "该规则没有配置提示词，无法测试。");
        }
        try {
            LlmProvider provider = providerRouter.resolve(TEST_FEATURE_KEY);
            String model = providerRouter.resolveModel(TEST_FEATURE_KEY);
            Double temperature = rule.getParams() != null
                    ? asDouble(rule.getParams().get("temperature")) : null;
            Integer maxTokens = rule.getParams() != null
                    ? asInt(rule.getParams().get("max_tokens")) : null;

            String userPrompt = rule.getUserPrompt();
            if (StringUtils.isNotBlank(sampleInput) && StringUtils.isNotBlank(userPrompt)) {
                userPrompt = userPrompt.replace("{{input}}", sampleInput)
                        .replace("{{resume_text}}", sampleInput)
                        .replace("{{jd}}", sampleInput);
            }
            AiChatRequest request = AiChatRequest.builder()
                    .model(model)
                    .systemPrompt(rule.getSystemPrompt())
                    .userPrompt(userPrompt)
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .build();
            var response = provider.chat(request);
            return Map.of("ok", true, "model", model, "output", response.getContent());
        } catch (BusinessException e) {
            return Map.of("ok", false, "output", e.getMessage());
        } catch (Exception e) {
            log.warn("ai rule test run failed: ruleId={}", id, e);
            return Map.of("ok", false, "output", "测试运行失败：" + e.getMessage());
        }
    }

    private AiRule getRule(String id) {
        AiRule rule = aiRuleMapper.selectById(id);
        if (rule == null || BizConstant.DELETED.equals(rule.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "规则不存在。");
        }
        return rule;
    }

    private List<AiRule> listFamily(String familyId) {
        return aiRuleMapper.selectList(
                new LambdaQueryWrapper<AiRule>()
                        .eq(AiRule::getFamilyId, familyId)
                        .eq(AiRule::getDeleted, BizConstant.NOT_DELETED)
                        .orderByDesc(AiRule::getVersion));
    }

    private void applyRequest(AiRule rule, AiRuleRequest request) {
        if (request == null) {
            return;
        }
        rule.setName(request.getName() == null ? rule.getName() : request.getName().trim());
        rule.setRuleType(request.getRuleType() == null ? rule.getRuleType() : request.getRuleType());
        rule.setDescription(request.getDescription());
        rule.setSystemPrompt(request.getSystemPrompt());
        rule.setUserPrompt(request.getUserPrompt());
        rule.setParams(request.getParams());
    }

    private void validateType(String ruleType) {
        List<String> types = List.of("score", "prompt", "security", "dict", "match", "risk");
        if (!types.contains(ruleType)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "规则类型不合法。");
        }
    }

    private long countByStatus(String status) {
        LambdaQueryWrapper<AiRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiRule::getDeleted, BizConstant.NOT_DELETED);
        if (status != null) {
            wrapper.eq(AiRule::getStatus, status);
        }
        return aiRuleMapper.selectCount(wrapper);
    }

    private Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}