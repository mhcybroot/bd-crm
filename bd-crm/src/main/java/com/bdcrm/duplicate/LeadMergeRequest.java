package com.bdcrm.duplicate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeadMergeRequest(
        @NotNull Long sourceLeadId,
        @NotNull Long targetLeadId,
        @Size(max = 500) String summary) {
}
