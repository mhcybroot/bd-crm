package com.bdcrm.template;

import java.util.List;

public record FollowupTemplateResponse(
        Long id,
        String name,
        String description,
        boolean isDefault,
        boolean active,
        List<TemplateStepResponse> steps) {

    public static FollowupTemplateResponse from(FollowupTemplate template) {
        return new FollowupTemplateResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.isDefault(),
                template.isActive(),
                template.getSteps().stream().map(TemplateStepResponse::from).toList());
    }
}
