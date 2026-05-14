package com.bdcrm.template;

import com.bdcrm.auth.SecurityUtils;
import com.bdcrm.common.ApiException;
import com.bdcrm.organization.Organization;
import com.bdcrm.pipeline.PipelineService;
import com.bdcrm.pipeline.StageDefinitionRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateService {

    private final FollowupTemplateRepository templateRepository;
    private final PipelineService pipelineService;
    private final SecurityUtils securityUtils;

    @Autowired
    public TemplateService(FollowupTemplateRepository templateRepository, PipelineService pipelineService, SecurityUtils securityUtils) {
        this.templateRepository = templateRepository;
        this.pipelineService = pipelineService;
        this.securityUtils = securityUtils;
    }

    public TemplateService(FollowupTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
        this.pipelineService = null;
        this.securityUtils = null;
    }

    @Transactional(readOnly = true)
    public List<FollowupTemplateResponse> listTemplates() {
        return scopedTemplates().stream()
                .peek(this::initializeCollections)
                .map(FollowupTemplateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FollowupTemplate getTemplateEntity(Long id) {
        FollowupTemplate template = scopedTemplateById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Template not found"));
        initializeCollections(template);
        return template;
    }

    @Transactional(readOnly = true)
    public FollowupTemplate getDefaultTemplateEntity() {
        FollowupTemplate template = scopedDefaultTemplate()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No default active template configured"));
        initializeCollections(template);
        return template;
    }

    @Transactional
    public FollowupTemplateResponse createTemplate(FollowupTemplateRequest request) {
        Organization organization = currentOrganization();
        FollowupTemplate template = new FollowupTemplate();
        template.setOrganization(organization);
        apply(template, request);
        return FollowupTemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public FollowupTemplateResponse updateTemplate(Long templateId, FollowupTemplateRequest request) {
        FollowupTemplate template = getTemplateEntity(templateId);
        template.getSteps().clear();
        apply(template, request);
        return FollowupTemplateResponse.from(template);
    }

    private void apply(FollowupTemplate template, FollowupTemplateRequest request) {
        validateSteps(request.steps());
        if (request.isDefault()) {
            scopedTemplates().stream()
                    .filter(FollowupTemplate::isDefault)
                    .filter(existing -> template.getId() == null || !existing.getId().equals(template.getId()))
                    .forEach(existing -> existing.setDefault(false));
        }
        template.setName(request.name().trim());
        template.setDescription(request.description());
        template.setDefault(request.isDefault());
        template.setActive(request.active());
        request.steps().stream()
                .sorted(java.util.Comparator.comparingInt(TemplateStepRequest::stepNumber))
                .forEach(stepRequest -> {
                    FollowupTemplateStep step = new FollowupTemplateStep();
                    step.setTemplate(template);
                    step.setOrganization(template.getOrganization());
                    step.setStepNumber(stepRequest.stepNumber());
                    step.setDayOffset(stepRequest.dayOffset());
                    step.setChannel(stepRequest.channel());
                    step.setInstructions(stepRequest.instructions());
                    template.getSteps().add(step);
                });
        validateStages(request.stages());
        Objects.requireNonNull(pipelineService, "PipelineService is required for template persistence")
                .syncTemplateStages(template, request.stages());
    }

    public void validateSteps(List<TemplateStepRequest> steps) {
        if (steps.size() > 7) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A template can contain at most 7 follow-up steps");
        }
        Set<Integer> stepNumbers = new HashSet<>();
        for (TemplateStepRequest step : steps) {
            if (!stepNumbers.add(step.stepNumber())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Duplicate follow-up step numbers are not allowed");
            }
        }
    }

    public Optional<FollowupTemplate> findDefaultTemplate() {
        return scopedDefaultTemplate();
    }

    private void validateStages(List<StageDefinitionRequest> stages) {
        Set<Integer> orders = new HashSet<>();
        for (StageDefinitionRequest stage : stages) {
            if (!orders.add(stage.stageOrder())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Duplicate pipeline stage order values are not allowed");
            }
        }
    }

    private void initializeCollections(FollowupTemplate template) {
        template.getSteps().size();
        template.getPipelineStages().size();
    }

    private List<FollowupTemplate> scopedTemplates() {
        if (securityUtils != null && !securityUtils.hasPlatformRole("PLATFORM_ADMIN")) {
            return templateRepository.findAllByOrganizationIdOrderByNameAsc(securityUtils.currentOrganizationId());
        }
        return templateRepository.findAllByOrderByNameAsc();
    }

    private Optional<FollowupTemplate> scopedTemplateById(Long templateId) {
        if (securityUtils != null && !securityUtils.hasPlatformRole("PLATFORM_ADMIN")) {
            return templateRepository.findByIdAndOrganizationId(templateId, securityUtils.currentOrganizationId());
        }
        return templateRepository.findById(templateId);
    }

    private Optional<FollowupTemplate> scopedDefaultTemplate() {
        if (securityUtils != null && !securityUtils.hasPlatformRole("PLATFORM_ADMIN")) {
            return templateRepository.findFirstByOrganizationIdAndIsDefaultTrueAndActiveTrue(securityUtils.currentOrganizationId());
        }
        return templateRepository.findFirstByIsDefaultTrueAndActiveTrue();
    }

    private Organization currentOrganization() {
        if (securityUtils == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Security context unavailable");
        }
        return securityUtils.currentOrganizationEntity();
    }
}
