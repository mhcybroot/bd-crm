package com.bdcrm.lead;

import com.bdcrm.pipeline.LeadStageUpdateRequest;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkLeadActionRequest(
        @NotEmpty List<Long> leadIds,
        Long assignedUserId,
        LeadStatus status,
        Long stageId) {
}
