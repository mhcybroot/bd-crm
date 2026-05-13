package com.bdcrm.dashboard;

import java.util.Map;

public record DashboardSummaryResponse(
        long leadsTotal,
        long newLeadsInRange,
        Map<String, Long> leadsByStatus,
        long dueToday,
        long overdue,
        long escalated,
        long completedFollowups,
        long completionRate,
        Map<String, Long> topOutcomes) {
}
