package com.resume.pdf.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.resume.common.constant.BizConstant;
import com.resume.pdf.entity.PdfTask;
import com.resume.pdf.mapper.PdfTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PDF 导出任务僵尸态恢复。
 * <p>
 * 服务重启后 pending/processing 任务永久卡死、前端无限轮询的问题：
 * </p>
 * <ul>
 *   <li>启动时（ApplicationRunner）把遗留的所有非终态任务一次性标记 failed；</li>
 *   <li>运行期每 10 分钟将 updated_at 超时仍 pending/processing 的任务置 failed。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfTaskRecoveryJob implements ApplicationRunner {

    /** 进行中状态超过该分钟数仍未更新即视为僵尸任务。 */
    @Value("${app.pdf.stale-task-minutes:30}")
    private int staleTaskMinutes;

    private final PdfTaskMapper pdfTaskMapper;

    @Override
    public void run(ApplicationArguments args) {
        // 启动只清僵尸（updated_at < cutoff），避免滚动发布误杀健康任务
        int recovered = failStaleTasks(LocalDateTime.now().minusMinutes(staleTaskMinutes));
        if (recovered > 0) {
            log.warn("PDF task recovery on startup: marked {} leftover non-terminal tasks as failed", recovered);
        }
    }

    /**
     * 周期回收僵尸任务，防止任务执行线程丢失/超时导致前端永远轮询。
     */
    @Scheduled(fixedDelayString = "${app.pdf.recovery-interval-ms:600000}",
            initialDelayString = "${app.pdf.recovery-initial-delay-ms:60000}")
    public void recoverStaleTasks() {
        int recovered = failStaleTasks(LocalDateTime.now().minusMinutes(staleTaskMinutes));
        if (recovered > 0) {
            log.warn("PDF stale task recovery: marked {} tasks (updated_at older than {} min) as failed",
                    recovered, staleTaskMinutes);
        }
    }

    /**
     * 将 updated_at 早于 cutoff 的 pending/processing 任务批量置为 failed。
     *
     * @param cutoff 启动恢复传 {@link LocalDateTime#MAX} 表示不限时间（全部非终态任务）
     * @return 受影响行数
     */
    public int failStaleTasks(LocalDateTime cutoff) {
        LambdaUpdateWrapper<PdfTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(PdfTask::getStatus,
                        List.of(BizConstant.TASK_STATUS_PENDING, BizConstant.TASK_STATUS_PROCESSING))
                .lt(PdfTask::getUpdatedAt, cutoff)
                .set(PdfTask::getStatus, BizConstant.TASK_STATUS_FAILED)
                .set(PdfTask::getErrorMsg, "服务中断导致导出任务未完成，请重新发起导出。")
                .set(PdfTask::getUpdatedAt, LocalDateTime.now());
        Integer affected = pdfTaskMapper.update(null, wrapper);
        return affected == null ? 0 : affected;
    }

    /**
     * 启动时一次性把全部非终态遗留任务置为 failed。
     * 不传 LocalDateTime.MAX 给 SQL 是为了规避 MySQL Connector/J 8.3 在写入极大时间时
     * 由于 serverTimezone 调整发生 Year 1000000000 异常。这里直接省掉时间条件。
     */
    public int failAllStaleTasks() {
        LambdaUpdateWrapper<PdfTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(PdfTask::getStatus,
                        List.of(BizConstant.TASK_STATUS_PENDING, BizConstant.TASK_STATUS_PROCESSING))
                .set(PdfTask::getStatus, BizConstant.TASK_STATUS_FAILED)
                .set(PdfTask::getErrorMsg, "服务中断导致导出任务未完成，请重新发起导出。")
                .set(PdfTask::getUpdatedAt, LocalDateTime.now());
        Integer affected = pdfTaskMapper.update(null, wrapper);
        return affected == null ? 0 : affected;
    }
}
