package com.bdcrm.lead;

import jakarta.validation.constraints.NotNull;

public record LeadStatusUpdateRequest(@NotNull LeadStatus status) {
}
