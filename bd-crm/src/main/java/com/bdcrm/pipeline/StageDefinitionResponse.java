package com.bdcrm.pipeline;

public record StageDefinitionResponse(
        Long id,
        String name,
        int stageOrder,
        int slaHours,
        String exitAutomation) {

    public static StageDefinitionResponse from(TemplatePipelineStage stage) {
        return new StageDefinitionResponse(
                stage.getId(),
                stage.getName(),
                stage.getStageOrder(),
                stage.getSlaHours(),
                stage.getExitAutomation());
    }
}
