package com.bdcrm.pipeline;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StageDefinitionRequest(
        @NotBlank String name,
        @Min(1) int stageOrder,
        @Min(1) int slaHours,
        @Size(max = 120) String exitAutomation) {
}
