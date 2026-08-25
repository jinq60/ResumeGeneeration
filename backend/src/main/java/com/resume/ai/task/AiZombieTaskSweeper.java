package com.resume.ai.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.resume.ai.entity.ResumeOptimizeTask;
import com.resume.ai.mapper.ResumeOptimizeTaskMapper;
import com.resume.common.constant.BizConstant;
import com.resume.resume.entity.ResumeReview;
import com.resume.resume.mapper.ResumeReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 僵尸任务定时清理。
 * <p>
 * 服务重启 / 异步线程池丢任务时，pending / processing 状态的行会永久滞留，
 * 既让用户任务不可感知，也会占用 {@code AiResumeOptimizeService} 的每用户并发额度。
 * 本任务周期性把 {@code updated_at} 超过阈值的滞留任务批量置为 failed。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiZombieTaskSweeper {

    /** 超过该时长仍未流转的任务视为僵尸任务。 */
    public static final Duration ZOMBIE_THRESHOLD = Duration.ofMinutes(30);

    /** 定时扫描间隔：10 分钟（注解要求编译期常量，不能用 Duration 运行时计算）。 */
    static final long SWEEP_INTERVAL_MS = 600_000L;

    private final ResumeOptimizeTaskMapper optimizeTaskMapper;
    private final ResumeReviewMapper resumeReviewMapper;

    /**
     * 每 10 分钟扫描一次，把 updated_at 超过 30 分钟仍 pending/processing 的
     * JD 优化与简历点评任务批量置为 failed。
     */
    @Scheduled(fixedDelay = SWEEP_INTERVAL_MS)
    public void sweepZombieTasks() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minus(ZOMBIE_THRESHOLD);

        int optimizeCount = optimizeTaskMapper.update(null, new LambdaUpdateWrapper<ResumeOptimizeTask>()
                .in(ResumeOptimizeTask::getStatus, List.of(
                        BizConstant.TASK_STATUS_PENDING, BizConstant.TASK_STATUS_PROCESSING))
                .lt(ResumeOptimizeTask::getUpdatedAt, threshold)
                .set(ResumeOptimizeTask::getStatus, BizConstant.TASK_STATUS_FAILED)
                .set(ResumeOptimizeTask::getErrorMsg, "服务中断，请重试")
                .set(ResumeOptimizeTask::getUpdatedAt, now));

        int reviewCount = resumeReviewMapper.update(null, new LambdaUpdateWrapper<ResumeReview>()
                .in(ResumeReview::getStatus, List.of(
                        BizConstant.TASK_STATUS_PENDING, BizConstant.TASK_STATUS_PROCESSING))
                .lt(ResumeReview::getUpdatedAt, threshold)
                .set(ResumeReview::getStatus, BizConstant.REVIEW_STATUS_FAILED)
                .set(ResumeReview::getErrorMsg, "服务中断，请重试")
                .set(ResumeReview::getUpdatedAt, now));

        if (optimizeCount > 0 || reviewCount > 0) {
            log.info("zombie ai tasks swept: optimize={}, review={}, threshold={}",
                    optimizeCount, reviewCount, threshold);
        }
    }
}
