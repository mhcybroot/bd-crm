package com.bdcrm.pipeline;

import com.bdcrm.lead.LeadSummaryResponse;
import java.util.List;

public record PipelineStageLeadPageResponse(
        Long stageId,
        String stageName,
        long totalElements,
        int totalPages,
        int page,
        int size,
        List<LeadSummaryResponse> content) {
}
