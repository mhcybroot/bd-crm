package com.bdcrm.search;

import com.bdcrm.attachment.AttachmentRecordRepository;
import com.bdcrm.followup.LeadFollowupRepository;
import com.bdcrm.lead.LeadNoteRepository;
import com.bdcrm.lead.LeadActivityRepository;
import com.bdcrm.lead.LeadRepository;
import com.bdcrm.lead.LeadStatus;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final LeadRepository leadRepository;
    private final LeadNoteRepository noteRepository;
    private final LeadActivityRepository activityRepository;
    private final LeadFollowupRepository followupRepository;
    private final AttachmentRecordRepository attachmentRepository;

    @Transactional(readOnly = true)
    public GlobalSearchResponse search(
            String q,
            Long owner,
            LeadStatus status,
            Long stageId,
            String source,
            String outcome,
            LocalDate dateFrom,
            LocalDate dateTo) {
        String query = q == null ? "" : q.trim().toLowerCase();
        return new GlobalSearchResponse(
                leadRepository.findAll().stream()
                        .filter(lead -> lead.getMergedIntoLeadId() == null)
                        .filter(lead -> query.isBlank() || lead.getCompanyName().toLowerCase().contains(query) || lead.getContactName().toLowerCase().contains(query))
                        .filter(lead -> owner == null || lead.getAssignedUser().getId().equals(owner))
                        .filter(lead -> status == null || lead.getStatus() == status)
                        .filter(lead -> stageId == null || (lead.getCurrentStage() != null && lead.getCurrentStage().getId().equals(stageId)))
                        .filter(lead -> source == null || source.isBlank() || (lead.getSource() != null && lead.getSource().equalsIgnoreCase(source)))
                        .map(lead -> new GlobalSearchResponse.SearchItem("LEAD", lead.getId(), lead.getId(), lead.getCompanyName(), lead.getContactName()))
                        .limit(10)
                        .toList(),
                noteRepository.findAll().stream()
                        .filter(note -> query.isBlank() || note.getBody().toLowerCase().contains(query))
                        .map(note -> new GlobalSearchResponse.SearchItem("NOTE", note.getId(), note.getLead().getId(), note.getAuthor().getFullName(), note.getBody()))
                        .limit(10)
                        .toList(),
                activityRepository.findAll().stream()
                        .filter(activity -> query.isBlank() || activity.getDescription().toLowerCase().contains(query))
                        .map(activity -> new GlobalSearchResponse.SearchItem("ACTIVITY", activity.getId(), activity.getLead().getId(), activity.getType().name(), activity.getDescription()))
                        .limit(10)
                        .toList(),
                followupRepository.findAll().stream()
                        .filter(followup -> query.isBlank() || (followup.getNotes() != null && followup.getNotes().toLowerCase().contains(query)))
                        .filter(followup -> outcome == null || outcome.isBlank() || (followup.getOutcome() != null && followup.getOutcome().name().equalsIgnoreCase(outcome)))
                        .map(followup -> new GlobalSearchResponse.SearchItem("FOLLOWUP", followup.getId(), followup.getLead().getId(), "Follow-up " + followup.getStepNumber(), followup.getStatus().name()))
                        .limit(10)
                        .toList(),
                attachmentRepository.findAll().stream()
                        .filter(attachment -> query.isBlank() || attachment.getOriginalFileName().toLowerCase().contains(query))
                        .map(attachment -> new GlobalSearchResponse.SearchItem("ATTACHMENT", attachment.getId(), attachment.getLead() != null ? attachment.getLead().getId() : null, attachment.getOriginalFileName(), attachment.getContentType()))
                        .limit(10)
                        .toList());
    }
}
