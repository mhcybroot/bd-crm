package com.bdcrm.lead;

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
        String templateName) {

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
                lead.getTemplate().getName());
    }
}
