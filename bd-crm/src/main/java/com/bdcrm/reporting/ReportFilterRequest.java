package com.bdcrm.reporting;

import com.bdcrm.followup.FollowupOutcome;
import com.bdcrm.lead.LeadPriority;
import com.bdcrm.lead.LeadStatus;
import com.bdcrm.template.ContactChannel;
import java.time.LocalDate;

public record ReportFilterRequest(
        LocalDate dateFrom,
        LocalDate dateTo,
        Long repUserId,
        LeadStatus leadStatus,
        FollowupOutcome followupOutcome,
        String source,
        Long templateId,
        ContactChannel channel,
        LeadPriority priority,
        Boolean escalated,
        String sortBy,
        ReportSortDirection sortDirection) {

    public LocalDate effectiveDateFrom() {
        return dateFrom == null ? LocalDate.now().minusDays(30) : dateFrom;
    }

    public LocalDate effectiveDateTo() {
        return dateTo == null ? LocalDate.now() : dateTo;
    }

    public ReportSortDirection effectiveSortDirection() {
        return sortDirection == null ? ReportSortDirection.DESC : sortDirection;
    }

    public String effectiveSortBy() {
        return sortBy == null || sortBy.isBlank() ? "completedFollowups" : sortBy;
    }
}
