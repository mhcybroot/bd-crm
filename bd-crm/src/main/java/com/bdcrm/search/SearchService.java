package com.bdcrm.search;

import com.bdcrm.attachment.AttachmentRecordRepository;
import com.bdcrm.attachment.AttachmentSpecifications;
import com.bdcrm.followup.FollowupSpecifications;
import com.bdcrm.followup.LeadFollowupRepository;
import com.bdcrm.lead.ActivitySpecifications;
import com.bdcrm.lead.LeadActivityRepository;
import com.bdcrm.lead.LeadNoteRepository;
import com.bdcrm.lead.LeadRepository;
import com.bdcrm.lead.LeadSpecifications;
import com.bdcrm.lead.NoteSpecifications;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int DEFAULT_LIMIT = 10;

    private final LeadRepository leadRepository;
    private final LeadNoteRepository noteRepository;
    private final LeadActivityRepository activityRepository;
    private final LeadFollowupRepository followupRepository;
    private final AttachmentRecordRepository attachmentRepository;

    @Transactional(readOnly = true)
    public GlobalSearchResponse search(
            String q,
            Long owner,
            com.bdcrm.lead.LeadStatus status,
            Long stageId,
            String source,
            String outcome,
            LocalDate dateFrom,
            LocalDate dateTo) {

        String query = q == null ? "" : q.trim();

        return new GlobalSearchResponse(
                searchLeads(query, owner, status, stageId, source, dateFrom, dateTo),
                searchNotes(query),
                searchActivities(query),
                searchFollowups(query, outcome),
                searchAttachments(query));
    }

    private java.util.List<GlobalSearchResponse.SearchItem> searchLeads(
            String query,
            Long owner,
            com.bdcrm.lead.LeadStatus status,
            Long stageId,
            String source,
            LocalDate dateFrom,
            LocalDate dateTo) {

        Specification<com.bdcrm.lead.Lead> spec = Specification.where(LeadSpecifications.notMerged())
                .and(LeadSpecifications.search(query))
                .and(LeadSpecifications.assignedTo(owner))
                .and(LeadSpecifications.hasStatus(status))
                .and(LeadSpecifications.currentStage(stageId))
                .and(LeadSpecifications.source(source))
                .and(LeadSpecifications.createdBetween(dateFrom, dateTo));

        Page<com.bdcrm.lead.Lead> page = leadRepository.findAll(spec, Pageable.ofSize(DEFAULT_LIMIT));
        return page.getContent().stream()
                .map(lead -> new GlobalSearchResponse.SearchItem(
                        "LEAD",
                        lead.getId(),
                        lead.getId(),
                        lead.getCompanyName(),
                        lead.getContactName()))
                .toList();
    }

    private java.util.List<GlobalSearchResponse.SearchItem> searchNotes(String query) {
        Specification<com.bdcrm.lead.LeadNote> spec = NoteSpecifications.search(query);
        Page<com.bdcrm.lead.LeadNote> page = noteRepository.findAll(spec, Pageable.ofSize(DEFAULT_LIMIT));
        return page.getContent().stream()
                .map(note -> new GlobalSearchResponse.SearchItem(
                        "NOTE",
                        note.getId(),
                        note.getLead().getId(),
                        note.getAuthor().getFullName(),
                        note.getBody()))
                .toList();
    }

    private java.util.List<GlobalSearchResponse.SearchItem> searchActivities(String query) {
        Specification<com.bdcrm.lead.LeadActivity> spec = ActivitySpecifications.search(query);
        Page<com.bdcrm.lead.LeadActivity> page = activityRepository.findAll(spec, Pageable.ofSize(DEFAULT_LIMIT));
        return page.getContent().stream()
                .map(activity -> new GlobalSearchResponse.SearchItem(
                        "ACTIVITY",
                        activity.getId(),
                        activity.getLead().getId(),
                        activity.getType().name(),
                        activity.getDescription()))
                .toList();
    }

    private java.util.List<GlobalSearchResponse.SearchItem> searchFollowups(String query, String outcome) {
        Specification<com.bdcrm.followup.LeadFollowup> spec = Specification.where(FollowupSpecifications.search(query))
                .and(FollowupSpecifications.hasOutcome(outcome));
        Page<com.bdcrm.followup.LeadFollowup> page = followupRepository.findAll(spec, Pageable.ofSize(DEFAULT_LIMIT));
        return page.getContent().stream()
                .map(followup -> new GlobalSearchResponse.SearchItem(
                        "FOLLOWUP",
                        followup.getId(),
                        followup.getLead().getId(),
                        "Follow-up " + followup.getStepNumber(),
                        followup.getStatus().name()))
                .toList();
    }

    private java.util.List<GlobalSearchResponse.SearchItem> searchAttachments(String query) {
        Specification<com.bdcrm.attachment.AttachmentRecord> spec = AttachmentSpecifications.search(query);
        Page<com.bdcrm.attachment.AttachmentRecord> page = attachmentRepository.findAll(spec, Pageable.ofSize(DEFAULT_LIMIT));
        return page.getContent().stream()
                .map(attachment -> new GlobalSearchResponse.SearchItem(
                        "ATTACHMENT",
                        attachment.getId(),
                        attachment.getLead() != null ? attachment.getLead().getId() : null,
                        attachment.getOriginalFileName(),
                        attachment.getContentType()))
                .toList();
    }
}