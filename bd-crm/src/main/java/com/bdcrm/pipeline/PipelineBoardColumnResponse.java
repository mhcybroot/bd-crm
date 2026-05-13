package com.bdcrm.pipeline;

public record PipelineBoardColumnResponse(
        Long stageId,
        String stageName,
        int slaHours,
        long leadCount,
        long slaBreachCount) {
}
