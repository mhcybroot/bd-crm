package com.bdcrm.commandcenter;

import com.bdcrm.duplicate.DuplicateCandidateResponse;
import com.bdcrm.followup.LeadFollowupResponse;
import com.bdcrm.notification.NotificationResponse;
import java.util.List;

public record CommandCenterResponse(
        List<NotificationResponse> notifications,
        List<LeadFollowupResponse> dueFollowups,
        List<LeadFollowupResponse> overdueFollowups,
        List<DuplicateCandidateResponse> duplicates,
        List<String> recommendations) {
}
