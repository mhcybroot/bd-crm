package com.bdcrm.search;

import com.bdcrm.attachment.AttachmentRecordRepository;
import com.bdcrm.auth.SecurityUtils;
import com.bdcrm.followup.LeadFollowupRepository;
import com.bdcrm.lead.LeadNoteRepository;
import com.bdcrm.lead.LeadActivityRepository;
import com.bdcrm.lead.Lead;
import com.bdcrm.lead.LeadRepository;
import com.bdcrm.lead.LeadSpecifications;
import com.bdcrm.lead.LeadStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
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
    private final SecurityUtils securityUtils;

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
        String query = normalize(q);
        Long organizationId = securityUtils.hasPlatformRole("PLATFORM_ADMIN") ? null : securityUtils.currentOrganizationId();
        Specification<Lead> leadSpec = Specification.where(LeadSpecifications.organizationId(organizationId))
                .and(LeadSpecifications.notMerged())
                .and(LeadSpecifications.assignedTo(owner))
                .and(LeadSpecifications.hasStatus(status))
                .and(LeadSpecifications.currentStage(stageId))
                .and(LeadSpecifications.source(source))
                .and(LeadSpecifications.createdBetween(dateFrom, dateTo))
                .and(LeadSpecifications.search(query));
        List<Lead> matchingLeads = leadRepository.findAll(leadSpec);
        return new GlobalSearchResponse(
                matchingLeads.stream()
                        .map(lead -> new GlobalSearchResponse.SearchItem("LEAD", lead.getId(), lead.getId(), lead.getCompanyName(), lead.getContactName()))
                        .limit(10)
                        .toList(),
                noteRepository.findAll().stream()
                        .filter(note -> organizationId == null || note.getOrganization().getId().equals(organizationId))
                        .filter(note -> matchesLeadFilters(note.getLead(), owner, status, stageId, source, dateFrom, dateTo))
                        .filter(note -> query.isBlank()
                                || contains(note.getBody(), query)
                                || contains(note.getAuthor().getFullName(), query)
                                || matchesLeadSearch(note.getLead(), query))
                        .map(note -> new GlobalSearchResponse.SearchItem(
                                "NOTE",
                                note.getId(),
                                note.getLead().getId(),
                                leadContext(note.getLead()),
                                excerpt(note.getBody())))
                        .limit(10)
                        .toList(),
                activityRepository.findAll().stream()
                        .filter(activity -> organizationId == null || activity.getOrganization().getId().equals(organizationId))
                        .filter(activity -> matchesLeadFilters(activity.getLead(), owner, status, stageId, source, dateFrom, dateTo))
                        .filter(activity -> query.isBlank() || contains(activity.getDescription(), query))
                        .map(activity -> new GlobalSearchResponse.SearchItem(
                                "ACTIVITY",
                                activity.getId(),
                                activity.getLead().getId(),
                                activity.getType().name(),
                                excerpt(activity.getDescription())))
                        .limit(10)
                        .toList(),
                followupRepository.findAll().stream()
                        .filter(followup -> organizationId == null || followup.getOrganization().getId().equals(organizationId))
                        .filter(followup -> matchesLeadFilters(followup.getLead(), owner, status, stageId, source, dateFrom, dateTo))
                        .filter(followup -> query.isBlank()
                                || contains(followup.getNotes(), query)
                                || contains(followup.getInstructions(), query)
                                || contains(followup.getStatus().name(), query)
                                || contains(followup.getOutcome() != null ? followup.getOutcome().name() : null, query)
                                || matchesLeadSearch(followup.getLead(), query))
                        .filter(followup -> outcome == null || outcome.isBlank() || (followup.getOutcome() != null && followup.getOutcome().name().equalsIgnoreCase(outcome)))
                        .map(followup -> new GlobalSearchResponse.SearchItem(
                                "FOLLOWUP",
                                followup.getId(),
                                followup.getLead().getId(),
                                leadContext(followup.getLead()),
                                buildFollowupSubtitle(followup.getStepNumber(), followup.getStatus().name(),
                                        followup.getOutcome() != null ? followup.getOutcome().name() : null,
                                        firstNonBlank(followup.getNotes(), followup.getInstructions()))))
                        .limit(10)
                        .toList(),
                attachmentRepository.findAll().stream()
                        .filter(attachment -> organizationId == null || (attachment.getOrganization() != null
                                && attachment.getOrganization().getId().equals(organizationId)))
                        .filter(attachment -> query.isBlank() || contains(attachment.getOriginalFileName(), query))
                        .map(attachment -> new GlobalSearchResponse.SearchItem("ATTACHMENT", attachment.getId(), attachment.getLead() != null ? attachment.getLead().getId() : null, attachment.getOriginalFileName(), attachment.getContentType()))
                        .limit(10)
                        .toList());
    }

    private boolean matchesLeadFilters(
            Lead lead,
            Long owner,
            LeadStatus status,
            Long stageId,
            String source,
            LocalDate dateFrom,
            LocalDate dateTo) {
        return lead.getMergedIntoLeadId() == null
                && (owner == null || lead.getAssignedUser().getId().equals(owner))
                && (status == null || lead.getStatus() == status)
                && (stageId == null || (lead.getCurrentStage() != null && lead.getCurrentStage().getId().equals(stageId)))
                && (source == null || source.isBlank() || containsExact(lead.getSource(), source))
                && isWithinDateRange(lead.getCreatedAt(), dateFrom, dateTo);
    }

    private boolean matchesLeadSearch(Lead lead, String query) {
        return contains(lead.getCompanyName(), query)
                || contains(lead.getContactName(), query)
                || contains(lead.getEmail(), query)
                || contains(lead.getPhone(), query)
                || contains(lead.getSource(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && !query.isBlank() && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean containsExact(String value, String expected) {
        return value != null && value.equalsIgnoreCase(expected.trim());
    }

    private boolean isWithinDateRange(OffsetDateTime timestamp, LocalDate dateFrom, LocalDate dateTo) {
        if (timestamp == null) {
            return false;
        }
        LocalDate localDate = timestamp.atZoneSameInstant(ZoneOffset.UTC).toLocalDate();
        return (dateFrom == null || !localDate.isBefore(dateFrom))
                && (dateTo == null || !localDate.isAfter(dateTo));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String leadContext(Lead lead) {
        return lead.getCompanyName() + " - " + lead.getContactName();
    }

    private String buildFollowupSubtitle(int stepNumber, String status, String outcome, String detail) {
        StringBuilder builder = new StringBuilder("Step ").append(stepNumber).append(" - ").append(status);
        if (outcome != null && !outcome.isBlank()) {
            builder.append(" - ").append(outcome);
        }
        if (detail != null && !detail.isBlank()) {
            builder.append(" - ").append(excerpt(detail));
        }
        return builder.toString();
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback != null && !fallback.isBlank() ? fallback : null;
    }

    private String excerpt(String value) {
        if (value == null) {
            return null;
        }
        String collapsed = value.trim().replaceAll("\\s+", " ");
        if (collapsed.length() <= 96) {
            return collapsed;
        }
        return collapsed.substring(0, 93) + "...";
    }
}
