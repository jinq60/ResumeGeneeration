package com.resume.audit.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审核操作请求。
 */
@Data
public class AuditReviewRequest {

    @Size(max = 512, message = "审核备注最长 512 字符")
    private String note;

    /** 标记风险时使用：low / medium / high，缺省 low。 */
    @Size(max = 16, message = "风险等级不合法")
    private String riskLevel;
}