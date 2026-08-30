package com.resume.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.notification.dto.NotificationResponse;
import com.resume.notification.entity.Notification;
import com.resume.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户通知业务服务。
 * <p>
 * 通知由系统事件触发（PDF 导出完成、AI 点评完成、头像优化完成等），
 * 通过 {@link #notify(String, String, String, String)} 写入。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    /**
     * 写入一条通知。
     *
     * @param userId  接收用户 ID
     * @param type    类型：pdf / avatar / ai / system
     * @param title   标题
     * @param content 内容
     */
    @Transactional(rollbackFor = Exception.class)
    public void notify(String userId, String type, String title, String content) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReadFlag(0);
        notification.setDeleted(BizConstant.NOT_DELETED);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    /**
     * 分页查询通知。
     */
    public Page<NotificationResponse> list(String userId, int page, int size, boolean unreadOnly) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getDeleted, BizConstant.NOT_DELETED);
        if (unreadOnly) {
            wrapper.eq(Notification::getReadFlag, 0);
        }
        wrapper.orderByDesc(Notification::getCreatedAt);

        Page<Notification> pageParam = new Page<>(page, size);
        Page<Notification> result = notificationMapper.selectPage(pageParam, wrapper);

        Page<NotificationResponse> responsePage = new Page<>();
        responsePage.setRecords(result.getRecords().stream().map(NotificationResponse::from).toList());
        responsePage.setTotal(result.getTotal());
        responsePage.setCurrent(result.getCurrent());
        responsePage.setSize(result.getSize());
        responsePage.setPages(result.getPages());
        return responsePage;
    }

    /**
     * 未读通知数。
     */
    public long unreadCount(String userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getDeleted, BizConstant.NOT_DELETED)
                        .eq(Notification::getReadFlag, 0));
    }

    /**
     * 标记单条已读。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markRead(String userId, String notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || BizConstant.DELETED.equals(notification.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "通知不存在。");
        }
        if (!userId.equals(notification.getUserId())) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。");
        }
        Notification update = new Notification();
        update.setId(notificationId);
        update.setReadFlag(1);
        update.setUpdatedAt(LocalDateTime.now());
        notificationMapper.updateById(update);
    }

    /**
     * 全部标记已读。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(String userId) {
        // 使用 UpdateWrapper 字符串列名避免 Lambda 缓存初始化问题（单元测试 mock 环境下无 MyBatis 全局配置）
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Notification> wrapper =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("deleted", BizConstant.NOT_DELETED)
                .eq("read_flag", 0)
                .set("read_flag", 1)
                .set("updated_at", LocalDateTime.now());
        notificationMapper.update(null, wrapper);
    }

    /**
     * 删除通知（逻辑删除）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String userId, String notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || BizConstant.DELETED.equals(notification.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "通知不存在。");
        }
        if (!userId.equals(notification.getUserId())) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。");
        }
        // @TableLogic 下 deleteById 自动转为 UPDATE deleted=1；
        // setDeleted+updateById 会把逻辑删除字段排除在 SET 外导致静默失效，禁止使用
        notificationMapper.deleteById(notificationId);
    }
}