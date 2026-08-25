package com.resume.ai.util;

import com.resume.ai.entity.AiCallLog;
import org.apache.commons.lang3.StringUtils;

/**
 * AiCallLog 非空列兜底工具。
 * <p>
 * ai_call_log 的 provider_name / model_name / request_hash 为 NOT NULL 列；
 * 供应商 resolve 阶段抛异常时这些字段可能尚未填充，直接插入会失败导致失败调用进不了审计表。
 * 写入前统一调用 {@link #fillRequiredColumns} 兜底。
 * </p>
 */
public final class AiCallLogDefaults {

    public static final String UNKNOWN_PROVIDER = "unknown";

    private AiCallLogDefaults() {
    }

    /**
     * 为 NOT NULL 列填默认值：provider_name 优先用请求的目标 provider 名，否则 "unknown"；
     * model_name 兜底 "unknown"；request_hash 兜底空串。
     */
    public static void fillRequiredColumns(AiCallLog callLog, String targetProviderName) {
        if (callLog == null) {
            return;
        }
        if (StringUtils.isBlank(callLog.getProviderName())) {
            callLog.setProviderName(StringUtils.isNotBlank(targetProviderName)
                    ? targetProviderName : UNKNOWN_PROVIDER);
        }
        if (StringUtils.isBlank(callLog.getModelName())) {
            callLog.setModelName(UNKNOWN_PROVIDER);
        }
        if (callLog.getRequestHash() == null) {
            callLog.setRequestHash("");
        }
        if (callLog.getUserId() == null) {
            callLog.setUserId(UNKNOWN_PROVIDER);
        }
        if (callLog.getFeatureKey() == null) {
            callLog.setFeatureKey(UNKNOWN_PROVIDER);
        }
    }
}
