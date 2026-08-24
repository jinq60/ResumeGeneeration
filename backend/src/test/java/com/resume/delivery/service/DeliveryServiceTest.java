package com.resume.delivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.delivery.dto.DeliveryRecordRequest;
import com.resume.delivery.dto.DeliveryRecordResponse;
import com.resume.delivery.entity.DeliveryRecord;
import com.resume.delivery.mapper.DeliveryRecordMapper;
import com.resume.resume.entity.Resume;
import com.resume.resume.service.ResumeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeliveryServiceTest {

    @Mock
    private DeliveryRecordMapper deliveryRecordMapper;

    @Mock
    private ResumeService resumeService;

    private DeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        deliveryService = new DeliveryService(deliveryRecordMapper, resumeService);
    }

    private DeliveryRecordRequest buildRequest() {
        DeliveryRecordRequest request = new DeliveryRecordRequest();
        request.setResumeId("resume_1");
        request.setCompany("腾讯");
        request.setPosition("后端开发工程师");
        request.setChannel("官网");
        request.setStatus("delivered");
        request.setApplyDate(LocalDate.of(2026, 8, 1));
        request.setJdContent("负责高并发系统的设计与开发");
        return request;
    }

    @Test
    void create_shouldInsertRecordWithOwner() {
        Resume resume = new Resume();
        resume.setId("resume_1");
        resume.setUserId("user_1");
        when(resumeService.getResumeEntity("user_1", "resume_1")).thenReturn(resume);
        when(deliveryRecordMapper.insert(any(DeliveryRecord.class))).thenAnswer(inv -> {
            DeliveryRecord record = inv.getArgument(0);
            record.setId("delivery_1");
            return 1;
        });

        DeliveryRecordResponse response = deliveryService.create("user_1", buildRequest());

        assertEquals("delivery_1", response.getId());
        assertEquals("user_1", response.getUserId());
        assertEquals("腾讯", response.getCompany());
        ArgumentCaptor<DeliveryRecord> captor = ArgumentCaptor.forClass(DeliveryRecord.class);
        verify(deliveryRecordMapper).insert(captor.capture());
        assertEquals("user_1", captor.getValue().getUserId());
    }

    @Test
    void create_shouldRejectForeignResume() {
        when(resumeService.getResumeEntity("user_1", "resume_1"))
                .thenThrow(new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。"));

        assertThrows(BusinessException.class, () -> deliveryService.create("user_1", buildRequest()));
        verify(deliveryRecordMapper, never()).insert(any());
    }

    @Test
    void create_shouldRejectInvalidStatus() {
        Resume resume = new Resume();
        resume.setId("resume_1");
        resume.setUserId("user_1");
        when(resumeService.getResumeEntity("user_1", "resume_1")).thenReturn(resume);

        DeliveryRecordRequest request = buildRequest();
        request.setStatus("invalid_status");

        BusinessException ex = assertThrows(BusinessException.class, () -> deliveryService.create("user_1", request));
        assertEquals(ResultCode.PARAM_INVALID, ex.getErrorCode());
    }

    @Test
    void get_shouldReturnOwnedRecord() {
        DeliveryRecord record = new DeliveryRecord();
        record.setId("delivery_1");
        record.setUserId("user_1");
        record.setCompany("字节跳动");
        record.setPosition("前端开发工程师");
        when(deliveryRecordMapper.selectById("delivery_1")).thenReturn(record);

        DeliveryRecordResponse response = deliveryService.get("user_1", "delivery_1");

        assertEquals("delivery_1", response.getId());
        assertEquals("字节跳动", response.getCompany());
    }

    @Test
    void get_shouldRejectForeignRecord() {
        DeliveryRecord record = new DeliveryRecord();
        record.setId("delivery_1");
        record.setUserId("other_user");
        when(deliveryRecordMapper.selectById("delivery_1")).thenReturn(record);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deliveryService.get("user_1", "delivery_1"));
        assertEquals(ResultCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void update_shouldApplyNewStatus() {
        DeliveryRecord record = new DeliveryRecord();
        record.setId("delivery_1");
        record.setUserId("user_1");
        record.setStatus("delivered");
        when(deliveryRecordMapper.selectById("delivery_1")).thenReturn(record);

        DeliveryRecordRequest request = buildRequest();
        request.setStatus("interview1");
        request.setInterviewTime(java.time.LocalDateTime.of(2026, 8, 10, 10, 0));
        request.setInterviewLocation("深圳腾讯大厦");

        DeliveryRecordResponse response = deliveryService.update("user_1", "delivery_1", request);

        assertEquals("interview1", response.getStatus());
        assertEquals("深圳腾讯大厦", response.getInterviewLocation());
        verify(deliveryRecordMapper).updateById(any(DeliveryRecord.class));
    }

    @Test
    void delete_shouldLogicalDelete() {
        DeliveryRecord record = new DeliveryRecord();
        record.setId("delivery_1");
        record.setUserId("user_1");
        when(deliveryRecordMapper.selectById("delivery_1")).thenReturn(record);

        deliveryService.delete("user_1", "delivery_1");

        ArgumentCaptor<DeliveryRecord> captor = ArgumentCaptor.forClass(DeliveryRecord.class);
        verify(deliveryRecordMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getDeleted());
    }

    @Test
    void list_shouldReturnPagedRecords() {
        DeliveryRecord record = new DeliveryRecord();
        record.setId("delivery_1");
        record.setUserId("user_1");
        record.setCompany("华为");
        Page<DeliveryRecord> page = new Page<>();
        page.setRecords(List.of(record));
        page.setTotal(1);
        page.setCurrent(1);
        page.setSize(10);
        when(deliveryRecordMapper.selectPage(any(), any())).thenReturn(page);

        var result = deliveryService.list("user_1", 1, 10, null, null, null, null, null, null);

        assertEquals(1, result.getTotal());
        assertEquals("华为", result.getRecords().get(0).getCompany());
    }

    @Test
    void stats_shouldCountStatuses() {
        DeliveryRecord delivered = new DeliveryRecord();
        delivered.setStatus("delivered");
        delivered.setPosition("后端开发工程师");
        DeliveryRecord offer = new DeliveryRecord();
        offer.setStatus("offer");
        offer.setPosition("后端开发工程师");
        DeliveryRecord written = new DeliveryRecord();
        written.setStatus("written");
        written.setPosition("前端开发工程师");
        when(deliveryRecordMapper.selectList(any())).thenReturn(List.of(delivered, offer, written));

        var stats = deliveryService.stats();

        assertEquals(3, stats.getTotalDeliveries());
        assertEquals(1L, stats.getStatusCounts().get("delivered"));
        assertEquals(1L, stats.getStatusCounts().get("offer"));
        assertEquals(2L, stats.getTopJobs().get(0).getCount());
    }
}