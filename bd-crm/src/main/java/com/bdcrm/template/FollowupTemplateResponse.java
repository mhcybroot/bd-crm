package com.bdcrm.template;

import com.bdcrm.pipeline.StageDefinitionResponse;
import java.util.List;

public record FollowupTemplateResponse(
        Long id,
        String name,
        String description,
        boolean isDefault,
        boolean active,
        List<TemplateStepResponse> steps,
        List<StageDefinitionResponse> stages) {

    public static FollowupTemplateResponse from(FollowupTemplate template) {
        return new FollowupTemplateResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.isDefault(),
                template.isActive(),
                template.getSteps().stream().map(TemplateStepResponse::from).toList(),
                template.getPipelineStages().stream().map(StageDefinitionResponse::from).toList());
    }
}
