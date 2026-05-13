package com.bdcrm.followup;

import com.bdcrm.template.ContactChannel;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record LeadFollowupResponse(
        Long id,
        int stepNumber,
        LocalDate dueDate,
        Long assignedUserId,
        String assignedUserName,
        FollowupStatus status,
        ContactChannel channel,
        FollowupOutcome outcome,
        String instructions,
        String notes,
        OffsetDateTime completedAt,
        OffsetDateTime escalatedAt) {

    public static LeadFollowupResponse from(LeadFollowup followup) {
        return new LeadFollowupResponse(
                followup.getId(),
                followup.getStepNumber(),
                followup.getDueDate(),
                followup.getAssignedUser().getId(),
                followup.getAssignedUser().getFullName(),
                followup.getStatus(),
                followup.getChannel(),
                followup.getOutcome(),
                followup.getInstructions(),
                followup.getNotes(),
                followup.getCompletedAt(),
                followup.getEscalatedAt());
    }
}
