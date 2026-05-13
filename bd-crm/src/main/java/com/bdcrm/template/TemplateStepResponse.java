package com.bdcrm.template;

public record TemplateStepResponse(
        Long id,
        int stepNumber,
        int dayOffset,
        ContactChannel channel,
        String instructions) {

    public static TemplateStepResponse from(FollowupTemplateStep step) {
        return new TemplateStepResponse(
                step.getId(),
                step.getStepNumber(),
                step.getDayOffset(),
                step.getChannel(),
                step.getInstructions());
    }
}
