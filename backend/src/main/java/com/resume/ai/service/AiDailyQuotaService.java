package com.resume.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.resume.ai.entity.AiDailyQuota;
import com.resume.ai.mapper.AiDailyQuotaMapper;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * AI 功能每日配额计数。
 * <p>
 * 按「用户 × 功能 × 日期」持久化计数，更新使用原子条件（used_count &lt; limit）
 * 防止并发超卖；首次使用插入记录，插入竞态通过唯一索引冲突后重试更新解决。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiDailyQuotaService {

    private static final ZoneId ZONE_SHANGHAI = ZoneId.of("Asia/Shanghai");

    private final AiDailyQuotaMapper aiDailyQuotaMapper;

    private static String currentQuotaDate() {
        return LocalDate.now(ZONE_SHANGHAI).format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    /**
     * 消费一次配额，超出 {@code dailyLimit} 时抛 {@code AI_DAILY_QUOTA_EXCEEDED}。
     *
     * @param dailyLimit 当日上限；小于等于 0 表示功能不可用
     * @return 本次消费对应的 quotaDate（供失败时精准退款，避免跨天错表）
     */
    public String consume(String userId, String featureKey, int dailyLimit) {
        if (dailyLimit <= 0) {
            throw new BusinessException(ResultCode.AI_DAILY_QUOTA_EXCEEDED, "该 AI 功能今日已不可用。");
        }
        String quotaDate = currentQuotaDate();
        AiDailyQuota quota = find(userId, featureKey, quotaDate);
        if (quota == null) {
            if (!tryInsert(userId, featureKey, quotaDate)) {
                // 并发下另一请求已插入，走更新路径
                incrementOrFail(userId, featureKey, quotaDate, dailyLimit);
            }
            return quotaDate;
        }
        incrementOrFail(userId, featureKey, quotaDate, dailyLimit);
        return quotaDate;
    }

    /**
     * 退还一次配额（调用失败时使用）：used_count 减 1，下限保护为 0。
     * <p>仅当已有记录且 used_count &gt; 0 时生效；无记录时不做任何操作。</p>
     * <p>
     * 兼容旧调用：按业务时区当日退款，若未命中（跨天场景 23:59 消费 00:01 失败）则尝试昨日，避免配额永久虚高。
     * </p>
     */
    public void refund(String userId, String featureKey) {
        String today = currentQuotaDate();
        int updated = refundForDate(userId, featureKey, today);
        if (updated == 0) {
            // 跨天回退：消费在昨日，失败在今日
            String yesterday = LocalDate.now(ZONE_SHANGHAI).minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE);
            refundForDate(userId, featureKey, yesterday);
        }
    }

    /**
     * 按指定 quotaDate 精准退款，避免跨天错表。
     */
    public void refund(String userId, String featureKey, String quotaDate) {
        if (quotaDate == null || quotaDate.isBlank()) {
            refund(userId, featureKey);
            return;
        }
        refundForDate(userId, featureKey, quotaDate);
    }

    private int refundForDate(String userId, String featureKey, String quotaDate) {
        LambdaUpdateWrapper<AiDailyQuota> update = new LambdaUpdateWrapper<>();
        update.eq(AiDailyQuota::getUserId, userId)
                .eq(AiDailyQuota::getFeatureKey, featureKey)
                .eq(AiDailyQuota::getQuotaDate, quotaDate)
                .eq(AiDailyQuota::getDeleted, BizConstant.NOT_DELETED)
                .gt(AiDailyQuota::getUsedCount, 0)
                .setSql("used_count = GREATEST(used_count - 1, 0)");
        int updated = aiDailyQuotaMapper.update(null, update);
        if (updated == 0) {
            log.debug("AiDailyQuota refund skipped (no row or used_count=0): user={}, feature={}, date={}",
                    userId, featureKey, quotaDate);
        }
        return updated;
    }

    private AiDailyQuota find(String userId, String featureKey, String quotaDate) {
        LambdaQueryWrapper<AiDailyQuota> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiDailyQuota::getUserId, userId)
                .eq(AiDailyQuota::getFeatureKey, featureKey)
                .eq(AiDailyQuota::getQuotaDate, quotaDate)
                .eq(AiDailyQuota::getDeleted, BizConstant.NOT_DELETED)
                .last("LIMIT 1");
        return aiDailyQuotaMapper.selectOne(wrapper);
    }

    private boolean tryInsert(String userId, String featureKey, String quotaDate) {
        AiDailyQuota quota = new AiDailyQuota();
        quota.setUserId(userId);
        quota.setFeatureKey(featureKey);
        quota.setQuotaDate(quotaDate);
        quota.setUsedCount(1);
        quota.setDeleted(BizConstant.NOT_DELETED);
        quota.setCreatedAt(LocalDateTime.now());
        quota.setUpdatedAt(LocalDateTime.now());
        try {
            aiDailyQuotaMapper.insert(quota);
            return true;
        } catch (DuplicateKeyException e) {
            // 仅唯一索引冲突（并发插入同一 user+feature+date）走更新路径，
            // 其它数据完整性错误（如字段超长）继续抛出，避免被误判为并发冲突
            log.debug("AiDailyQuota insert race, retry via update: user={}, date={}", userId, quotaDate);
            return false;
        }
    }

    private void incrementOrFail(String userId, String featureKey, String quotaDate, int dailyLimit) {
        LambdaUpdateWrapper<AiDailyQuota> update = new LambdaUpdateWrapper<>();
        update.eq(AiDailyQuota::getUserId, userId)
                .eq(AiDailyQuota::getFeatureKey, featureKey)
                .eq(AiDailyQuota::getQuotaDate, quotaDate)
                .eq(AiDailyQuota::getDeleted, BizConstant.NOT_DELETED)
                .lt(AiDailyQuota::getUsedCount, dailyLimit)
                .setSql("used_count = used_count + 1");
        int updated = aiDailyQuotaMapper.update(null, update);
        if (updated == 0) {
            throw new BusinessException(ResultCode.AI_DAILY_QUOTA_EXCEEDED,
                    "今日 AI 写作次数已用完，请明天再来。");
        }
    }
}
