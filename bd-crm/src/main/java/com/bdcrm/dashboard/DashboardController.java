package com.bdcrm.dashboard;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(@ModelAttribute DashboardFilterRequest filter) {
        return dashboardService.summary(filter);
    }

    @GetMapping("/work-queue")
    public List<DueFollowupResponse> workQueue(@ModelAttribute DashboardFilterRequest filter) {
        return dashboardService.workQueue(filter);
    }
}
