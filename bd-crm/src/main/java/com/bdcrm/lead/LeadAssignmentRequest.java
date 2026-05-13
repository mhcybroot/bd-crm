package com.bdcrm.lead;

import jakarta.validation.constraints.NotNull;

public record LeadAssignmentRequest(@NotNull Long assignedUserId) {
}
