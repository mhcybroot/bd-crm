package com.bdcrm.pipeline;

import com.bdcrm.lead.LeadSummaryResponse;
import java.util.List;

public record PipelineBoardColumnResponse(
        Long stageId,
        String stageName,
        int slaHours,
        long leadCount,
        List<LeadSummaryResponse> leads) {
}
