package com.resume.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.delivery.dto.DeliveryRecordRequest;
import com.resume.delivery.dto.DeliveryRecordResponse;
import com.resume.delivery.dto.DeliveryStatsResponse;
import com.resume.delivery.entity.DeliveryRecord;
import com.resume.delivery.mapper.DeliveryRecordMapper;
import com.resume.resume.entity.Resume;
import com.resume.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 投递记录业务服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    /** 合法进度状态。 */
    public static final List<String> DELIVERY_STATUSES = List.of(
            "delivered", "written", "interview1", "interview2", "hr", "offer", "rejected", "withdrawn");

    private final DeliveryRecordMapper deliveryRecordMapper;
    private final ResumeService resumeService;

    /**
     * 创建投递记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public DeliveryRecordResponse create(String userId, DeliveryRecordRequest request) {
        Resume resume = resumeService.getResumeEntity(userId, request.getResumeId());
        validateStatus(request.getStatus());

        DeliveryRecord record = new DeliveryRecord();
        record.setUserId(userId);
        record.setResumeId(resume.getId());
        applyRequest(record, request);
        record.setDeleted(BizConstant.NOT_DELETED);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        deliveryRecordMapper.insert(record);

        log.info("delivery created: userId={}, recordId={}, company={}", userId, record.getId(), record.getCompany());
        return DeliveryRecordResponse.from(record);
    }

    /**
     * 分页查询当前用户投递记录。
     */
    public Page<DeliveryRecordResponse> list(String userId, int page, int size,
                                             String keyword, String company, String position,
                                             String status,
                                             LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<DeliveryRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeliveryRecord::getUserId, userId)
                .eq(DeliveryRecord::getDeleted, BizConstant.NOT_DELETED);
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(DeliveryRecord::getCompany, kw)
                    .or().like(DeliveryRecord::getPosition, kw));
        }
        if (StringUtils.isNotBlank(company)) {
            wrapper.like(DeliveryRecord::getCompany, company.trim());
        }
        if (StringUtils.isNotBlank(position)) {
            wrapper.like(DeliveryRecord::getPosition, position.trim());
        }
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(DeliveryRecord::getStatus, status);
        }
        if (startDate != null) {
            wrapper.ge(DeliveryRecord::getApplyDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(DeliveryRecord::getApplyDate, endDate);
        }
        wrapper.orderByDesc(DeliveryRecord::getApplyDate).orderByDesc(DeliveryRecord::getCreatedAt);

        Page<DeliveryRecord> pageParam = new Page<>(page, size);
        Page<DeliveryRecord> result = deliveryRecordMapper.selectPage(pageParam, wrapper);

        Page<DeliveryRecordResponse> responsePage = new Page<>();
        responsePage.setRecords(result.getRecords().stream().map(DeliveryRecordResponse::from).toList());
        responsePage.setTotal(result.getTotal());
        responsePage.setCurrent(result.getCurrent());
        responsePage.setSize(result.getSize());
        responsePage.setPages(result.getPages());
        return responsePage;
    }

    /**
     * 投递记录详情。
     */
    public DeliveryRecordResponse get(String userId, String recordId) {
        DeliveryRecord record = getOwnedRecord(userId, recordId);
        return DeliveryRecordResponse.from(record);
    }

    /**
     * 更新投递记录（进度 / 面试安排 / 备注等）。
     */
    @Transactional(rollbackFor = Exception.class)
    public DeliveryRecordResponse update(String userId, String recordId, DeliveryRecordRequest request) {
        DeliveryRecord record = getOwnedRecord(userId, recordId);
        applyRequest(record, request);
        record.setUpdatedAt(LocalDateTime.now());
        deliveryRecordMapper.updateById(record);
        return DeliveryRecordResponse.from(record);
    }

    /**
     * 删除投递记录（逻辑删除）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String userId, String recordId) {
        DeliveryRecord record = getOwnedRecord(userId, recordId);
        record.setDeleted(BizConstant.DELETED);
        record.setUpdatedAt(LocalDateTime.now());
        deliveryRecordMapper.updateById(record);
        log.info("delivery deleted: userId={}, recordId={}", userId, recordId);
    }

    private DeliveryRecord getOwnedRecord(String userId, String recordId) {
        DeliveryRecord record = deliveryRecordMapper.selectById(recordId);
        if (record == null || BizConstant.DELETED.equals(record.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "投递记录不存在。");
        }
        if (!userId.equals(record.getUserId())) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。");
        }
        return record;
    }

    private void applyRequest(DeliveryRecord record, DeliveryRecordRequest request) {
        record.setCompany(request.getCompany().trim());
        record.setPosition(request.getPosition().trim());
        record.setChannel(request.getChannel());
        record.setStatus(StringUtils.isNotBlank(request.getStatus()) ? request.getStatus() : "delivered");
        record.setApplyDate(request.getApplyDate() != null ? request.getApplyDate() : LocalDate.now());
        record.setJdContent(request.getJdContent());
        record.setNote(request.getNote());
        record.setInterviewTime(request.getInterviewTime());
        record.setInterviewLocation(request.getInterviewLocation());
    }

    private void validateStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return;
        }
        if (!DELIVERY_STATUSES.contains(status)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "投递状态不合法。");
        }
    }

    // ---------------- 管理端 ----------------

    /**
     * 管理端分页查询全平台投递记录。
     */
    public Page<DeliveryRecordResponse> listAdmin(int page, int size, String keyword) {
        LambdaQueryWrapper<DeliveryRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeliveryRecord::getDeleted, BizConstant.NOT_DELETED);
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(DeliveryRecord::getCompany, kw)
                    .or().like(DeliveryRecord::getPosition, kw));
        }
        wrapper.orderByDesc(DeliveryRecord::getCreatedAt);

        Page<DeliveryRecord> pageParam = new Page<>(page, size);
        Page<DeliveryRecord> result = deliveryRecordMapper.selectPage(pageParam, wrapper);

        Page<DeliveryRecordResponse> responsePage = new Page<>();
        responsePage.setRecords(result.getRecords().stream().map(DeliveryRecordResponse::from).toList());
        responsePage.setTotal(result.getTotal());
        responsePage.setCurrent(result.getCurrent());
        responsePage.setSize(result.getSize());
        responsePage.setPages(result.getPages());
        return responsePage;
    }

    /**
     * 管理端投递统计：总数、状态分布、热门岗位 TOP5。
     */
    public DeliveryStatsResponse stats() {
        List<DeliveryRecord> records = deliveryRecordMapper.selectList(
                new LambdaQueryWrapper<DeliveryRecord>()
                        .eq(DeliveryRecord::getDeleted, BizConstant.NOT_DELETED));

        DeliveryStatsResponse response = new DeliveryStatsResponse();
        response.setTotalDeliveries(records.size());

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (String status : DELIVERY_STATUSES) {
            statusCounts.put(status, 0L);
        }
        records.forEach(r -> statusCounts.merge(r.getStatus() == null ? "delivered" : r.getStatus(), 1L, Long::sum));
        response.setStatusCounts(statusCounts);

        Map<String, Long> positionCounts = records.stream()
                .filter(r -> StringUtils.isNotBlank(r.getPosition()))
                .collect(Collectors.groupingBy(DeliveryRecord::getPosition, Collectors.counting()));
        long maxCount = positionCounts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        List<DeliveryStatsResponse.TopJob> topJobs = new ArrayList<>();
        positionCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> {
                    DeliveryStatsResponse.TopJob job = new DeliveryStatsResponse.TopJob();
                    job.setName(entry.getKey());
                    job.setCount(entry.getValue());
                    job.setPercent(maxCount == 0 ? 0 : Math.round(entry.getValue() * 1000.0 / maxCount) / 10.0);
                    topJobs.add(job);
                });
        response.setTopJobs(topJobs);
        return response;
    }

    /**
     * 管理端导出 CSV（全部未删除投递记录）。
     */
    public String buildCsv() {
        List<DeliveryRecord> records = deliveryRecordMapper.selectList(
                new LambdaQueryWrapper<DeliveryRecord>()
                        .eq(DeliveryRecord::getDeleted, BizConstant.NOT_DELETED)
                        .orderByDesc(DeliveryRecord::getCreatedAt));
        StringBuilder sb = new StringBuilder();
        sb.append("id,user_id,resume_id,company,position,channel,status,apply_date,interview_time,created_at\n");
        for (DeliveryRecord r : records) {
            sb.append(csv(r.getId())).append(',')
                    .append(csv(r.getUserId())).append(',')
                    .append(csv(r.getResumeId())).append(',')
                    .append(csv(r.getCompany())).append(',')
                    .append(csv(r.getPosition())).append(',')
                    .append(csv(r.getChannel())).append(',')
                    .append(csv(r.getStatus())).append(',')
                    .append(r.getApplyDate() != null ? r.getApplyDate() : "").append(',')
                    .append(r.getInterviewTime() != null ? r.getInterviewTime() : "").append(',')
                    .append(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                    .append('\n');
        }
        return sb.toString();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        // 防 Excel 公式注入：以 = + - @ 或制表符开头的用户可控值前置单引号
        String safe = value;
        if (!safe.isEmpty() && ("=+-@\t".indexOf(safe.charAt(0)) >= 0)) {
            safe = "'" + safe;
        }
        String escaped = safe.replace("\"", "\"\"");
        return escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")
                ? "\"" + escaped + "\"" : escaped;
    }
}