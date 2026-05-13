package com.bdcrm.duplicate;

public record DuplicateCandidateResponse(
        Long id,
        Long leadId,
        String leadCompanyName,
        Long matchedLeadId,
        String matchedLeadCompanyName,
        int matchScore,
        DuplicateState state,
        String reason) {

    public static DuplicateCandidateResponse from(DuplicateMatch match) {
        return new DuplicateCandidateResponse(
                match.getId(),
                match.getLead().getId(),
                match.getLead().getCompanyName(),
                match.getMatchedLead().getId(),
                match.getMatchedLead().getCompanyName(),
                match.getMatchScore(),
                match.getState(),
                match.getReason());
    }
}
