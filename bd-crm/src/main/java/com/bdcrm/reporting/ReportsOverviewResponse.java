package com.bdcrm.reporting;

import java.util.List;
import java.util.Map;

public record ReportsOverviewResponse(
        ReportKpiSummaryResponse summary,
        Map<String, Long> funnel,
        OutcomeSummaryResponse outcomes,
        List<TrendPointResponse> trends,
        List<PerformanceReportResponse.RepPerformance> reps) {
}
