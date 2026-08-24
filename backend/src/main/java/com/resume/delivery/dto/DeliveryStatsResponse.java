package com.resume.delivery.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 管理端投递数据统计。
 */
@Data
public class DeliveryStatsResponse {

    /** 总投递数。 */
    private long totalDeliveries;

    /** 各进度状态数量。 */
    private Map<String, Long> statusCounts;

    /** 热门岗位 TOP5。 */
    private List<TopJob> topJobs;

    @Data
    public static class TopJob {
        private String name;
        private long count;
        private double percent;
    }
}