package com.bdcrm.lead;

public record LeadScoreSummaryResponse(
        int fitScore,
        int engagementScore,
        int totalScore) {
}
