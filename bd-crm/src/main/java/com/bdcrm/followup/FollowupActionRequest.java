package com.bdcrm.followup;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record FollowupActionRequest(
        @FutureOrPresent LocalDate dueDate,
        FollowupOutcome outcome,
        Long assignedUserId,
        @Size(max = 2000) String notes) {
}
