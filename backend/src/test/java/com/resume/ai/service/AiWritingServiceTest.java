package com.resume.ai.service;

import com.resume.ai.config.AiPromptTemplates;
import com.resume.ai.dto.AiChatRequest;
import com.resume.ai.dto.AiChatResponse;
import com.resume.ai.dto.ResumeAiWriteRequest;
import com.resume.ai.mapper.AiCallLogMapper;
import com.resume.ai.provider.LlmProvider;
import com.resume.ai.provider.ProviderRouter;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.AuditLogService;
import com.resume.resume.dto.SectionDTO;
import com.resume.resume.entity.Resume;
import com.resume.resume.service.ResumeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiWritingServiceTest {

    @Mock
    private ResumeService resumeService;

    @Mock
    private ProviderRouter providerRouter;

    @Mock
    private AiPromptTemplates promptTemplates;

    @Mock
    private AiCallLogMapper aiCallLogMapper;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private LlmProvider llmProvider;

    private AiWritingService service;

    @BeforeEach
    void setUp() {
        service = new AiWritingService(resumeService, providerRouter, promptTemplates,
                aiCallLogMapper, auditLogService, new ObjectMapper());
        when(providerRouter.resolve(anyString())).thenReturn(llmProvider);
        when(providerRouter.resolveModel(anyString())).thenReturn("qwen-turbo");
        when(promptTemplates.render(anyString(), any())).thenReturn("prompt");
        when(llmProvider.getProviderName()).thenReturn("qwen");
    }

    private Resume buildResume(String userId, String resumeId) {
        Resume resume = new Resume();
        resume.setId(resumeId);
        resume.setUserId(userId);
        resume.setTargetPosition("Java 后端开发");
        Map<String, Object> intro = new HashMap<>();
        intro.put("content", "现有自我介绍内容");
        SectionDTO section = new SectionDTO();
        section.setId("s1");
        section.setType("introduction");
        section.setTitle("自我介绍");
        section.setOrder(0);
        section.setVisible(true);
        section.setData(intro);
        resume.setSections(List.of(section));
        return resume;
    }

    private ResumeAiWriteRequest buildRequest(String action) {
        ResumeAiWriteRequest request = new ResumeAiWriteRequest();
        request.setSectionType("introduction");
        request.setField("content");
        request.setAction(action);
        request.setOriginalText("现有自我介绍内容");
        if ("translate".equals(action)) {
            request.setTargetLang("en");
        }
        return request;
    }

    @Test
    void write_shouldReturnGeneratedContent() {
        when(resumeService.getResumeEntity("user_1", "resume_1"))
                .thenReturn(buildResume("user_1", "resume_1"));
        AiChatResponse response = new AiChatResponse();
        response.setContent("优化后的自我介绍");
        response.setSuccess(true);
        when(llmProvider.chat(any(AiChatRequest.class))).thenReturn(response);

        var result = service.write("user_1", "resume_1", buildRequest("polish"));

        assertEquals("优化后的自我介绍", result.getContent());
    }

    @Test
    void write_shouldRejectFieldOutsideWhitelist() {
        when(resumeService.getResumeEntity("user_1", "resume_1"))
                .thenReturn(buildResume("user_1", "resume_1"));
        ResumeAiWriteRequest request = buildRequest("polish");
        request.setField("hackerField");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.write("user_1", "resume_1", request));
        assertEquals(ResultCode.AI_WRITING_FIELD_INVALID, ex.getErrorCode());
    }

    @Test
    void write_shouldRejectUnknownAction() {
        when(resumeService.getResumeEntity("user_1", "resume_1"))
                .thenReturn(buildResume("user_1", "resume_1"));
        ResumeAiWriteRequest request = buildRequest("polish");
        request.setAction("hack");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.write("user_1", "resume_1", request));
        assertEquals(ResultCode.AI_WRITING_FIELD_INVALID, ex.getErrorCode());
    }

    @Test
    void write_shouldRejectTranslateWithoutTargetLang() {
        when(resumeService.getResumeEntity("user_1", "resume_1"))
                .thenReturn(buildResume("user_1", "resume_1"));
        ResumeAiWriteRequest request = buildRequest("translate");
        request.setTargetLang(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.write("user_1", "resume_1", request));
        assertEquals(ResultCode.AI_WRITING_FIELD_INVALID, ex.getErrorCode());
    }

    @Test
    void write_shouldRejectOverlongOriginalText() {
        when(resumeService.getResumeEntity("user_1", "resume_1"))
                .thenReturn(buildResume("user_1", "resume_1"));
        ResumeAiWriteRequest request = buildRequest("polish");
        request.setOriginalText("x".repeat(2001));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.write("user_1", "resume_1", request));
        assertEquals(ResultCode.AI_WRITING_CONTENT_TOO_LONG, ex.getErrorCode());
    }

    @Test
    void write_shouldPropagateAccessDeniedFromResumeService() {
        when(resumeService.getResumeEntity("user_2", "resume_1"))
                .thenThrow(new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.write("user_2", "resume_1", buildRequest("polish")));
        assertEquals(ResultCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void write_shouldFailWhenProviderReturnsError() {
        when(resumeService.getResumeEntity("user_1", "resume_1"))
                .thenReturn(buildResume("user_1", "resume_1"));
        AiChatResponse response = new AiChatResponse();
        response.setSuccess(false);
        response.setErrorMsg("provider error");
        when(llmProvider.chat(any(AiChatRequest.class))).thenReturn(response);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.write("user_1", "resume_1", buildRequest("polish")));
        assertEquals(ResultCode.AI_MODEL_CALL_FAILED, ex.getErrorCode());
    }

    @Test
    void write_shouldFailWhenContentEmpty() {
        when(resumeService.getResumeEntity("user_1", "resume_1"))
                .thenReturn(buildResume("user_1", "resume_1"));
        AiChatResponse response = new AiChatResponse();
        response.setContent("   ");
        response.setSuccess(true);
        when(llmProvider.chat(any(AiChatRequest.class))).thenReturn(response);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.write("user_1", "resume_1", buildRequest("polish")));
        assertEquals(ResultCode.AI_RESPONSE_PARSE_FAILED, ex.getErrorCode());
    }

    @Test
    void stream_shouldReturnProviderDeltas() {
        when(resumeService.getResumeEntity("user_1", "resume_1"))
                .thenReturn(buildResume("user_1", "resume_1"));
        when(llmProvider.stream(any(AiChatRequest.class)))
                .thenReturn(Flux.just("第一段", "第二段"));

        List<String> result = service.stream("user_1", "resume_1", buildRequest("polish"))
                .collectList()
                .block();

        assertEquals(List.of("第一段", "第二段"), result);
    }
}
