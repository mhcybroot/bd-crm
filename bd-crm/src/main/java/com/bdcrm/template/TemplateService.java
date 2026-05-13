package com.bdcrm.template;

import com.bdcrm.common.ApiException;
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

    @Autowired
    public TemplateService(FollowupTemplateRepository templateRepository, PipelineService pipelineService) {
        this.templateRepository = templateRepository;
        this.pipelineService = pipelineService;
    }

    public TemplateService(FollowupTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
        this.pipelineService = null;
    }

    @Transactional(readOnly = true)
    public List<FollowupTemplateResponse> listTemplates() {
        return templateRepository.findAllByOrderByNameAsc().stream()
                .peek(this::initializeCollections)
                .map(FollowupTemplateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FollowupTemplate getTemplateEntity(Long id) {
        FollowupTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Template not found"));
        initializeCollections(template);
        return template;
    }

    @Transactional(readOnly = true)
    public FollowupTemplate getDefaultTemplateEntity() {
        FollowupTemplate template = templateRepository.findFirstByIsDefaultTrueAndActiveTrue()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No default active template configured"));
        initializeCollections(template);
        return template;
    }

    @Transactional
    public FollowupTemplateResponse createTemplate(FollowupTemplateRequest request) {
        FollowupTemplate template = new FollowupTemplate();
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
            templateRepository.findAllByOrderByNameAsc().stream()
                    .filter(FollowupTemplate::isDefault)
                    .filter(existing -> !existing.getId().equals(template.getId()))
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
        return templateRepository.findFirstByIsDefaultTrueAndActiveTrue();
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
}
