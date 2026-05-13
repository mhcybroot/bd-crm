package com.bdcrm.pipeline;

import com.bdcrm.lead.LeadPriority;
import com.bdcrm.lead.LeadStatus;
import java.time.LocalDate;

public record PipelineBoardFilterRequest(
        String search,
        Long assignedUserId,
        LeadPriority priority,
        LeadStatus leadStatus,
        String source,
        LocalDate dateFrom,
        LocalDate dateTo) {
}
