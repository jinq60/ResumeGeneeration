package com.resume.notification.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.notification.dto.NotificationResponse;
import com.resume.notification.entity.Notification;
import com.resume.notification.mapper.NotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private NotificationMapper notificationMapper;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationMapper);
    }

    @Test
    void notify_shouldInsertUnreadNotification() {
        when(notificationMapper.insert(any(Notification.class))).thenAnswer(inv -> {
            Notification notification = inv.getArgument(0);
            notification.setId("n_1");
            return 1;
        });

        notificationService.notify("user_1", "pdf", "PDF 导出完成", "简历已导出");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        assertEquals("user_1", captor.getValue().getUserId());
        assertEquals(0, captor.getValue().getReadFlag());
        assertEquals("pdf", captor.getValue().getType());
    }

    @Test
    void notify_shouldIgnoreBlankUserId() {
        notificationService.notify("", "pdf", "t", "c");
        verify(notificationMapper, never()).insert(any());
    }

    @Test
    void list_shouldReturnPagedNotifications() {
        Notification notification = new Notification();
        notification.setId("n_1");
        notification.setUserId("user_1");
        notification.setReadFlag(0);
        Page<Notification> page = new Page<>();
        page.setRecords(List.of(notification));
        page.setTotal(1);
        when(notificationMapper.selectPage(any(), any())).thenReturn(page);

        Page<NotificationResponse> result = notificationService.list("user_1", 1, 20, false);

        assertEquals(1, result.getTotal());
        assertFalse(result.getRecords().get(0).isRead());
    }

    @Test
    void markRead_shouldUpdateReadFlag() {
        Notification notification = new Notification();
        notification.setId("n_1");
        notification.setUserId("user_1");
        when(notificationMapper.selectById("n_1")).thenReturn(notification);

        notificationService.markRead("user_1", "n_1");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getReadFlag());
    }

    @Test
    void markRead_shouldRejectForeignNotification() {
        Notification notification = new Notification();
        notification.setId("n_1");
        notification.setUserId("other_user");
        when(notificationMapper.selectById("n_1")).thenReturn(notification);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.markRead("user_1", "n_1"));
        assertEquals(ResultCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void markAllRead_shouldUpdateUnreadOnly() {
        notificationService.markAllRead("user_1");

        verify(notificationMapper).update(any(), any());
    }

    @Test
    void delete_shouldLogicalDelete() {
        Notification notification = new Notification();
        notification.setId("n_1");
        notification.setUserId("user_1");
        when(notificationMapper.selectById("n_1")).thenReturn(notification);

        notificationService.delete("user_1", "n_1");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getDeleted());
    }
}