package com.bdcrm.reporting;

import java.util.List;
import java.util.Map;

public record PerformanceReportResponse(
        Map<String, Long> leadFunnel,
        long completionRate,
        OutcomeSummaryResponse outcomes,
        List<TrendPointResponse> trends,
        List<RepPerformance> reps) {

    public record RepPerformance(
            Long userId,
            String userName,
            long assignedLeads,
            long pendingFollowups,
            long completedFollowups) {
    }
}
