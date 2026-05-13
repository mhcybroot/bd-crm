package com.bdcrm.dashboard;

import com.bdcrm.followup.FollowupStatus;
import com.bdcrm.template.ContactChannel;
import java.time.LocalDate;

public record DueFollowupResponse(
        Long followupId,
        Long leadId,
        String companyName,
        String contactName,
        int stepNumber,
        LocalDate dueDate,
        FollowupStatus status,
        ContactChannel channel,
        Long assignedUserId,
        String assignedUserName,
        boolean escalated) {
}
