package com.resume.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.ai.config.AiPromptTemplates;
import com.resume.ai.config.AiProperties;
import com.resume.ai.dto.AiChatRequest;
import com.resume.ai.dto.GrammarCheckResponse;
import com.resume.ai.mapper.AiCallLogMapper;
import com.resume.ai.provider.LlmProvider;
import com.resume.ai.provider.ProviderRouter;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.AuditLogService;
import com.resume.resume.entity.Resume;
import com.resume.resume.service.ResumeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiGrammarServiceTest {

    @Mock
    private ResumeService resumeService;
    @Mock
    private AiProperties aiProperties;
    @Mock
    private ProviderRouter providerRouter;
    @Mock
    private AiPromptTemplates promptTemplates;
    @Mock
    private AiCallLogMapper aiCallLogMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private LlmProvider provider;

    private AiGrammarService service;

    @BeforeEach
    void setUp() {
        service = new AiGrammarService(resumeService, aiProperties, providerRouter, promptTemplates,
                aiCallLogMapper, auditLogService, new ObjectMapper());
        when(resumeService.getResumeEntity("user_1", "resume_1")).thenReturn(new Resume());
        when(providerRouter.resolve(AiGrammarService.FEATURE_KEY)).thenReturn(provider);
        when(providerRouter.resolveModel(AiGrammarService.FEATURE_KEY)).thenReturn("qwen-turbo");
        when(provider.getProviderName()).thenReturn("qwen");
        AiProperties.QwenConfig qwenConfig = new AiProperties.QwenConfig();
        qwenConfig.setApiKey("test-key");
        when(aiProperties.getQwen()).thenReturn(qwenConfig);
        AiProperties.FeatureConfig featureConfig = new AiProperties.FeatureConfig();
        featureConfig.setProvider("qwen");
        featureConfig.setModel("qwen-turbo");
        when(providerRouter.getFeatureConfig(AiGrammarService.FEATURE_KEY)).thenReturn(featureConfig);
        when(promptTemplates.render(eq(AiGrammarService.FEATURE_KEY), any())).thenReturn("prompt");
    }

    @Test
    void check_shouldReturnStructuredIssues() {
        GrammarCheckResponse result = new GrammarCheckResponse();
        GrammarCheckResponse.GrammarIssue issue = new GrammarCheckResponse.GrammarIssue();
        issue.setSectionType("introduction");
        issue.setField("content");
        issue.setSeverity("medium");
        issue.setSuggestion("改得更专业");
        result.setIssues(List.of(issue));
        when(provider.chatStructured(any(AiChatRequest.class), eq(GrammarCheckResponse.class)))
                .thenReturn(result);

        GrammarCheckResponse response = service.check("user_1", "resume_1");

        assertEquals("success", response.getStatus());
        assertEquals(1, response.getIssues().size());
        assertEquals("改得更专业", response.getIssues().get(0).getSuggestion());
    }

    @Test
    void check_shouldReturnUnavailableWhenProviderIsNotConfigured() {
        when(providerRouter.resolve(AiGrammarService.FEATURE_KEY))
                .thenThrow(new BusinessException(ResultCode.AI_PROVIDER_NOT_CONFIGURED, "未配置"));

        GrammarCheckResponse response = service.check("user_1", "resume_1");

        assertEquals("unavailable", response.getStatus());
        assertTrue(response.getIssues().isEmpty());
    }

    @Test
    void check_shouldPropagateResumeAccessError() {
        when(resumeService.getResumeEntity("user_2", "resume_1"))
                .thenThrow(new BusinessException(ResultCode.ACCESS_DENIED, "无权访问"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.check("user_2", "resume_1"));
        assertEquals(ResultCode.ACCESS_DENIED, exception.getErrorCode());
    }
}
