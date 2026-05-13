package com.bdcrm.lead;

import com.bdcrm.followup.LeadFollowupResponse;
import java.util.List;

public record LeadDetailResponse(
        LeadSummaryResponse lead,
        List<LeadFollowupResponse> followups,
        List<LeadNoteResponse> notes,
        List<LeadActivityResponse> activities) {
}
