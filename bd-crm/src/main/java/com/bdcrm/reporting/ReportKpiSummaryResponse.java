package com.bdcrm.reporting;

public record ReportKpiSummaryResponse(
        long leadsInRange,
        long followupsInRange,
        long followupsCompleted,
        long overdueFollowups,
        long completionRate,
        long wonLeads,
        long lostLeads) {
}
