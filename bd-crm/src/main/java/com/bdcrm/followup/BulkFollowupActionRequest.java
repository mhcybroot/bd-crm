package com.bdcrm.followup;

import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.List;

public record BulkFollowupActionRequest(
        @NotEmpty List<Long> followupIds,
        String action,
        LocalDate dueDate,
        Long assignedUserId,
        String notes) {
}
