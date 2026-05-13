package com.bdcrm.qualification;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record LeadQualificationRequest(
        @Size(max = 120) String budgetRange,
        @Size(max = 120) String authorityLevel,
        String needSummary,
        @Size(max = 120) String timelineTarget,
        @Min(0) @Max(100) int fitScore,
        @Min(0) @Max(100) int engagementScore,
        String qualificationNotes) {
}
