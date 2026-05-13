package com.bdcrm.dashboard;

import com.bdcrm.followup.FollowupOutcome;
import com.bdcrm.lead.LeadStatus;
import java.time.LocalDate;

public record DashboardFilterRequest(
        LocalDate dateFrom,
        LocalDate dateTo,
        Long repUserId,
        LeadStatus leadStatus,
        FollowupOutcome followupOutcome) {

    public LocalDate effectiveDateFrom() {
        return dateFrom == null ? LocalDate.now().minusDays(30) : dateFrom;
    }

    public LocalDate effectiveDateTo() {
        return dateTo == null ? LocalDate.now() : dateTo;
    }
}
