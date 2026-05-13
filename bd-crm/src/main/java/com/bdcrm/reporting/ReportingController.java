package com.bdcrm.reporting;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ReportsOverviewResponse overview(@ModelAttribute ReportFilterRequest filter) {
        return reportingService.overview(filter);
    }

    @GetMapping("/funnel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, Long> funnel(@ModelAttribute ReportFilterRequest filter) {
        return reportingService.funnel(filter);
    }

    @GetMapping("/followup-performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public PerformanceReportResponse performance(@ModelAttribute ReportFilterRequest filter) {
        return reportingService.performance(filter);
    }

    @GetMapping("/outcomes")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OutcomeSummaryResponse outcomes(@ModelAttribute ReportFilterRequest filter) {
        return reportingService.performance(filter).outcomes();
    }
}
