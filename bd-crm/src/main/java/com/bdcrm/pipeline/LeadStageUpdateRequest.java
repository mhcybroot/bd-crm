package com.bdcrm.pipeline;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeadStageUpdateRequest(
        @NotNull Long stageId,
        @Size(max = 500) String note) {
}
