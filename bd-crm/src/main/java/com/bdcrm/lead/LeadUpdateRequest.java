package com.bdcrm.lead;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeadUpdateRequest(
        @NotBlank String companyName,
        @NotBlank String contactName,
        @Email String email,
        String phone,
        String source,
        @Size(max = 2000) String description,
        LeadPriority priority,
        Long assignedUserId,
        Long templateId) {
}
