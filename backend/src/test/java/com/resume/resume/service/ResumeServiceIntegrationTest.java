package com.resume.resume.service;

import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.resume.dto.CreateResumeRequest;
import com.resume.resume.dto.DuplicateResumeResponse;
import com.resume.resume.dto.ResumeDetailResponse;
import com.resume.resume.dto.UpdateResumeRequest;
import com.resume.template.entity.Template;
import com.resume.template.mapper.TemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试：需要外部 MySQL（默认 localhost:3307），通过 {@code RUN_INTEGRATION_TESTS=true} 显式启用。
 * <p>
 * 本地 {@code mvn test} 默认跳过；CI 在 {@code ops/docker-compose.test.yml} 启动测试库后通过
 * 环境变量启用本测试集。
 * </p>
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TESTS", matches = "true")
class ResumeServiceIntegrationTest {

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private TemplateMapper templateMapper;

    private String userId;
    private String templateId;

    @BeforeEach
    void setUp() {
        userId = "it_user_" + System.nanoTime();
        templateId = insertTestTemplate();
    }

    @Test
    void shouldCreateResumeAndRetrieveIt() {
        CreateResumeRequest request = new CreateResumeRequest();
        request.setTitle("集成测试简历");
        request.setScene(BizConstant.SCENE_SOCIAL_RECRUITMENT);
        request.setTargetPosition("Java开发工程师");
        request.setTemplateId(templateId);

        ResumeDetailResponse created = resumeService.createResume(userId, request);

        assertNotNull(created.getId());
        assertEquals("集成测试简历", created.getTitle());
        assertNotNull(created.getSections());
        assertFalse(created.getSections().isEmpty());

        ResumeDetailResponse retrieved = resumeService.getResume(userId, created.getId());
        assertEquals(created.getId(), retrieved.getId());
    }

    @Test
    void shouldUpdateResumeTitle() {
        CreateResumeRequest request = new CreateResumeRequest();
        request.setTitle("原始标题");
        request.setScene(BizConstant.SCENE_CAMPUS_RECRUITMENT);
        request.setTemplateId(templateId);

        ResumeDetailResponse created = resumeService.createResume(userId, request);

        UpdateResumeRequest update = new UpdateResumeRequest();
        update.setTitle("更新后的标题");
        resumeService.updateResume(userId, created.getId(), update);

        ResumeDetailResponse updated = resumeService.getResume(userId, created.getId());
        assertEquals("更新后的标题", updated.getTitle());
    }

    @Test
    void shouldDeleteAndNotFindResume() {
        CreateResumeRequest request = new CreateResumeRequest();
        request.setTitle("待删除");
        request.setScene(BizConstant.SCENE_SOCIAL_RECRUITMENT);
        request.setTemplateId(templateId);

        ResumeDetailResponse created = resumeService.createResume(userId, request);
        resumeService.deleteResume(userId, created.getId());

        assertThrows(BusinessException.class,
                () -> resumeService.getResume(userId, created.getId()));
    }

    @Test
    void shouldRejectOtherUserAccess() {
        CreateResumeRequest request = new CreateResumeRequest();
        request.setTitle("私人简历");
        request.setScene(BizConstant.SCENE_SOCIAL_RECRUITMENT);
        request.setTemplateId(templateId);

        ResumeDetailResponse created = resumeService.createResume(userId, request);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> resumeService.getResume("other_user", created.getId()));
        assertEquals(ResultCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void shouldDuplicateResume() {
        CreateResumeRequest request = new CreateResumeRequest();
        request.setTitle("原始简历");
        request.setScene(BizConstant.SCENE_SOCIAL_RECRUITMENT);
        request.setTemplateId(templateId);

        ResumeDetailResponse created = resumeService.createResume(userId, request);
        DuplicateResumeResponse result = resumeService.duplicateResume(userId, created.getId());

        assertNotNull(result.getId());
        assertTrue(result.getTitle().contains("副本"));
        assertNotEquals(created.getId(), result.getId());
    }

    private String insertTestTemplate() {
        Template template = new Template();
        template.setCode("it_tpl_" + System.nanoTime());
        template.setName("集成测试模板");
        template.setCategory("classic");
        template.setConfig(java.util.Map.of());
        template.setHtmlTemplate("test.html");
        template.setStatus(BizConstant.TEMPLATE_STATUS_ACTIVE);
        template.setIsBuiltin(BizConstant.BUILTIN_NO);
        template.setDeleted(BizConstant.NOT_DELETED);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.insert(template);
        return template.getId();
    }
}
