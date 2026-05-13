package com.bdcrm.lead;

import com.bdcrm.duplicate.DuplicateState;

public record LeadSummaryResponse(
        Long id,
        String companyName,
        String contactName,
        String email,
        String phone,
        String source,
        LeadStatus status,
        LeadPriority priority,
        Long assignedUserId,
        String assignedUserName,
        Long templateId,
        String templateName,
        Long currentStageId,
        String currentStageName,
        DuplicateState duplicateState) {

    public static LeadSummaryResponse from(Lead lead) {
        return new LeadSummaryResponse(
                lead.getId(),
                lead.getCompanyName(),
                lead.getContactName(),
                lead.getEmail(),
                lead.getPhone(),
                lead.getSource(),
                lead.getStatus(),
                lead.getPriority(),
                lead.getAssignedUser().getId(),
                lead.getAssignedUser().getFullName(),
                lead.getTemplate().getId(),
                lead.getTemplate().getName(),
                lead.getCurrentStage() != null ? lead.getCurrentStage().getId() : null,
                lead.getCurrentStage() != null ? lead.getCurrentStage().getName() : null,
                lead.getDuplicateState());
    }
}
