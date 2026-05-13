package com.bdcrm.template;

import com.bdcrm.common.ApiException;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final FollowupTemplateRepository templateRepository;

    public List<FollowupTemplateResponse> listTemplates() {
        return templateRepository.findAllByOrderByNameAsc().stream().map(FollowupTemplateResponse::from).toList();
    }

    public FollowupTemplate getTemplateEntity(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Template not found"));
    }

    public FollowupTemplate getDefaultTemplateEntity() {
        return templateRepository.findFirstByIsDefaultTrueAndActiveTrue()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No default active template configured"));
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
}
