package com.bdcrm.qualification;

import java.time.OffsetDateTime;

public record LeadQualificationResponse(
        Long id,
        String budgetRange,
        String authorityLevel,
        String needSummary,
        String timelineTarget,
        int fitScore,
        int engagementScore,
        int totalScore,
        String qualificationNotes,
        Long updatedByUserId,
        String updatedByUserName,
        OffsetDateTime qualificationUpdatedAt) {

    public static LeadQualificationResponse from(LeadQualification qualification) {
        return new LeadQualificationResponse(
                qualification.getId(),
                qualification.getBudgetRange(),
                qualification.getAuthorityLevel(),
                qualification.getNeedSummary(),
                qualification.getTimelineTarget(),
                qualification.getFitScore(),
                qualification.getEngagementScore(),
                qualification.getTotalScore(),
                qualification.getQualificationNotes(),
                qualification.getUpdatedBy() != null ? qualification.getUpdatedBy().getId() : null,
                qualification.getUpdatedBy() != null ? qualification.getUpdatedBy().getFullName() : null,
                qualification.getQualificationUpdatedAt());
    }
}
