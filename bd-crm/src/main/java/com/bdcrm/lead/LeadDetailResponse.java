package com.bdcrm.lead;

import com.bdcrm.attachment.AttachmentUploadResponse;
import com.bdcrm.attachment.DocumentLifecycleResponse;
import com.bdcrm.communication.LeadCommunicationResponse;
import com.bdcrm.followup.LeadFollowupResponse;
import com.bdcrm.pipeline.LeadStageHistoryResponse;
import com.bdcrm.qualification.LeadQualificationResponse;
import java.util.List;

public record LeadDetailResponse(
        LeadSummaryResponse lead,
        List<LeadFollowupResponse> followups,
        List<LeadNoteResponse> notes,
        List<LeadActivityResponse> activities,
        LeadQualificationResponse qualification,
        LeadScoreSummaryResponse score,
        List<LeadStageHistoryResponse> stageHistory,
        List<LeadCommunicationResponse> communications,
        List<AttachmentUploadResponse> attachments,
        List<DocumentLifecycleResponse> documents) {
}
