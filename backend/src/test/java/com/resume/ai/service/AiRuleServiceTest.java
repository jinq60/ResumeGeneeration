package com.resume.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.ai.dto.AiChatResponse;
import com.resume.ai.dto.AiRuleRequest;
import com.resume.ai.dto.AiRuleResponse;
import com.resume.ai.entity.AiRule;
import com.resume.ai.mapper.AiRuleMapper;
import com.resume.ai.provider.LlmProvider;
import com.resume.ai.provider.ProviderRouter;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiRuleServiceTest {

    @Mock
    private AiRuleMapper aiRuleMapper;

    @Mock
    private ProviderRouter providerRouter;

    private AiRuleService aiRuleService;

    @BeforeEach
    void setUp() {
        aiRuleService = new AiRuleService(aiRuleMapper, providerRouter);
    }

    private AiRuleRequest buildRequest() {
        AiRuleRequest request = new AiRuleRequest();
        request.setName("简历评分规则");
        request.setRuleType("score");
        request.setDescription("综合评分");
        request.setSystemPrompt("你是一名资深 HR");
        request.setUserPrompt("简历：{{resume_text}}");
        request.setParams(Map.of("temperature", 0.2));
        return request;
    }

    private AiRule buildRule(String id, int version, String status, String familyId) {
        AiRule rule = new AiRule();
        rule.setId(id);
        rule.setFamilyId(familyId);
        rule.setName("简历评分规则");
        rule.setRuleType("score");
        rule.setSystemPrompt("你是一名资深 HR");
        rule.setUserPrompt("简历：{{resume_text}}");
        rule.setVersion(version);
        rule.setStatus(status);
        return rule;
    }

    @Test
    void create_shouldInsertDraftWithFamilyId() {
        when(aiRuleMapper.insert(any(AiRule.class))).thenAnswer(inv -> {
            AiRule rule = inv.getArgument(0);
            rule.setId("rule_1");
            return 1;
        });

        AiRuleResponse response = aiRuleService.create("admin_1", buildRequest());

        assertEquals("rule_1", response.getId());
        assertEquals(1, response.getVersion());
        assertEquals("draft", response.getStatus());
        ArgumentCaptor<AiRule> captor = ArgumentCaptor.forClass(AiRule.class);
        verify(aiRuleMapper).insert(captor.capture());
        assertEquals("rule_1", captor.getValue().getId());
        // 创建后回写 family_id = 自身 ID
        ArgumentCaptor<AiRule> updateCaptor = ArgumentCaptor.forClass(AiRule.class);
        verify(aiRuleMapper).updateById(updateCaptor.capture());
        assertEquals("rule_1", updateCaptor.getValue().getFamilyId());
    }

    @Test
    void create_shouldRejectInvalidType() {
        AiRuleRequest request = buildRequest();
        request.setRuleType("bogus");

        BusinessException ex = assertThrows(BusinessException.class, () -> aiRuleService.create("admin_1", request));
        assertEquals(ResultCode.PARAM_INVALID, ex.getErrorCode());
    }

    @Test
    void publish_shouldCreateNextVersionAndDisableActive() {
        AiRule active = buildRule("rule_1", 1, "active", "family_1");
        when(aiRuleMapper.selectById("rule_1")).thenReturn(active);
        when(aiRuleMapper.selectList(any())).thenReturn(List.of(active));
        when(aiRuleMapper.insert(any(AiRule.class))).thenAnswer(inv -> {
            AiRule rule = inv.getArgument(0);
            rule.setId("rule_2");
            return 1;
        });

        AiRuleResponse response = aiRuleService.publish("admin_1", "rule_1", null);

        assertEquals("rule_2", response.getId());
        assertEquals(2, response.getVersion());
        assertEquals("active", response.getStatus());
        assertEquals("family_1", response.getFamilyId());
        // 旧版本被停用
        ArgumentCaptor<AiRule> captor = ArgumentCaptor.forClass(AiRule.class);
        verify(aiRuleMapper).updateById(captor.capture());
        assertEquals("disabled", captor.getValue().getStatus());
    }

    @Test
    void publish_shouldInheritMissingFieldsFromLatestVersion() {
        AiRule active = buildRule("rule_1", 1, "active", "family_1");
        active.setDescription("原描述");
        active.setSystemPrompt("原系统提示词");
        active.setUserPrompt("原用户提示词");
        active.setParams(Map.of("temperature", 0.2));
        when(aiRuleMapper.selectById("rule_1")).thenReturn(active);
        when(aiRuleMapper.selectList(any())).thenReturn(List.of(active));
        when(aiRuleMapper.insert(any(AiRule.class))).thenAnswer(inv -> {
            AiRule rule = inv.getArgument(0);
            rule.setId("rule_2");
            return 1;
        });

        AiRuleRequest request = new AiRuleRequest();
        request.setName("新名称");
        // description / systemPrompt / userPrompt / params 缺省，不应把生效中的值置 null
        AiRuleResponse response = aiRuleService.publish("admin_1", "rule_1", request);

        ArgumentCaptor<AiRule> captor = ArgumentCaptor.forClass(AiRule.class);
        verify(aiRuleMapper).insert(captor.capture());
        AiRule published = captor.getValue();
        assertEquals("新名称", response.getName());
        assertEquals("原描述", published.getDescription());
        assertEquals("原系统提示词", published.getSystemPrompt());
        assertEquals("原用户提示词", published.getUserPrompt());
        assertEquals(Map.of("temperature", 0.2), published.getParams());
    }

    @Test
    void toggle_shouldSwitchStatus() {
        AiRule rule = buildRule("rule_1", 1, "draft", "family_1");
        when(aiRuleMapper.selectById("rule_1")).thenReturn(rule);

        AiRuleResponse response = aiRuleService.toggle("admin_1", "rule_1", "active");

        assertEquals("active", response.getStatus());
        assertNotNull(response.getPublishedAt());
    }

    @Test
    void toggle_shouldRejectInvalidStatus() {
        AiRule rule = buildRule("rule_1", 1, "draft", "family_1");
        when(aiRuleMapper.selectById("rule_1")).thenReturn(rule);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> aiRuleService.toggle("admin_1", "rule_1", "bogus"));
        assertEquals(ResultCode.PARAM_INVALID, ex.getErrorCode());
    }

    @Test
    void rollback_shouldRestoreTargetVersion() {
        AiRule current = buildRule("rule_2", 2, "active", "family_1");
        AiRule v1 = buildRule("rule_1", 1, "disabled", "family_1");
        when(aiRuleMapper.selectById("rule_2")).thenReturn(current);
        when(aiRuleMapper.selectList(any())).thenReturn(List.of(current, v1));

        AiRuleResponse response = aiRuleService.rollback("admin_1", "rule_2", 1);

        assertEquals("rule_1", response.getId());
        assertEquals("active", response.getStatus());
    }

    @Test
    void rollback_shouldRejectMissingVersion() {
        AiRule current = buildRule("rule_2", 2, "active", "family_1");
        when(aiRuleMapper.selectById("rule_2")).thenReturn(current);
        when(aiRuleMapper.selectList(any())).thenReturn(List.of(current));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> aiRuleService.rollback("admin_1", "rule_2", 99));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void delete_shouldRejectActiveRule() {
        AiRule rule = buildRule("rule_1", 1, "active", "family_1");
        when(aiRuleMapper.selectById("rule_1")).thenReturn(rule);

        BusinessException ex = assertThrows(BusinessException.class, () -> aiRuleService.delete("rule_1"));
        assertEquals(ResultCode.PARAM_INVALID, ex.getErrorCode());
    }

    @Test
    void delete_shouldLogicalDeleteDraftViaDeleteById() {
        AiRule rule = buildRule("rule_1", 1, "draft", "family_1");
        when(aiRuleMapper.selectById("rule_1")).thenReturn(rule);

        aiRuleService.delete("rule_1");

        // @TableLogic 下必须走 deleteById（自动转 UPDATE deleted=1）
        verify(aiRuleMapper).deleteById("rule_1");
        verify(aiRuleMapper, never()).updateById(any(AiRule.class));
    }

    @Test
    void testRun_shouldReturnPlaceholderWhenNoPrompt() {
        AiRule rule = buildRule("rule_1", 1, "draft", "family_1");
        rule.setSystemPrompt(null);
        rule.setUserPrompt(null);
        when(aiRuleMapper.selectById("rule_1")).thenReturn(rule);

        Map<String, Object> result = aiRuleService.testRun("rule_1", "sample");

        assertEquals(Boolean.TRUE, result.get("ok"));
    }

    @Test
    void testRun_shouldCallProviderWhenConfigured() {
        AiRule rule = buildRule("rule_1", 1, "active", "family_1");
        when(aiRuleMapper.selectById("rule_1")).thenReturn(rule);

        LlmProvider provider = mock(LlmProvider.class);
        when(providerRouter.resolve("resume-review")).thenReturn(provider);
        when(providerRouter.resolveModel("resume-review")).thenReturn("qwen-turbo");
        AiChatResponse response = new AiChatResponse();
        response.setContent("评分结果");
        response.setSuccess(true);
        when(provider.chat(any())).thenReturn(response);

        Map<String, Object> result = aiRuleService.testRun("rule_1", "sample");

        assertEquals(Boolean.TRUE, result.get("ok"));
        assertEquals("评分结果", result.get("output"));
    }

    @Test
    void testRun_shouldReturnFailureMessageWhenProviderUnconfigured() {
        AiRule rule = buildRule("rule_1", 1, "active", "family_1");
        when(aiRuleMapper.selectById("rule_1")).thenReturn(rule);
        when(providerRouter.resolve("resume-review"))
                .thenThrow(new BusinessException(ResultCode.AI_PROVIDER_NOT_CONFIGURED, "未配置 AI 厂商"));

        Map<String, Object> result = aiRuleService.testRun("rule_1", "sample");

        assertEquals(Boolean.FALSE, result.get("ok"));
    }

    @Test
    void list_shouldReturnPagedRules() {
        AiRule rule = buildRule("rule_1", 1, "active", "family_1");
        Page<AiRule> page = new Page<>();
        page.setRecords(List.of(rule));
        page.setTotal(1);
        when(aiRuleMapper.selectPage(any(), any())).thenReturn(page);

        Page<AiRuleResponse> result = aiRuleService.list(1, 20, null, null, null);

        assertEquals(1, result.getTotal());
        assertEquals("简历评分规则", result.getRecords().get(0).getName());
    }
}