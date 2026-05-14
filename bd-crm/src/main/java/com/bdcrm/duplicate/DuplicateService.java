package com.bdcrm.duplicate;

import com.bdcrm.attachment.AttachmentRecordRepository;
import com.bdcrm.attachment.DocumentRecordRepository;
import com.bdcrm.audit.AuditEventService;
import com.bdcrm.auth.SecurityUtils;
import com.bdcrm.common.ApiException;
import com.bdcrm.communication.LeadCommunicationRepository;
import com.bdcrm.followup.LeadFollowup;
import com.bdcrm.followup.LeadFollowupRepository;
import com.bdcrm.lead.Lead;
import com.bdcrm.lead.LeadActivity;
import com.bdcrm.lead.LeadActivityRepository;
import com.bdcrm.lead.LeadActivityService;
import com.bdcrm.lead.LeadActivityType;
import com.bdcrm.lead.LeadNote;
import com.bdcrm.lead.LeadNoteRepository;
import com.bdcrm.lead.LeadRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DuplicateService {

    private final DuplicateMatchRepository duplicateMatchRepository;
    private final LeadMergeEventRepository mergeEventRepository;
    private final LeadRepository leadRepository;
    private final LeadNoteRepository noteRepository;
    private final LeadActivityRepository activityRepository;
    private final LeadFollowupRepository followupRepository;
    private final LeadCommunicationRepository communicationRepository;
    private final AttachmentRecordRepository attachmentRepository;
    private final DocumentRecordRepository documentRepository;
    private final SecurityUtils securityUtils;
    private final LeadActivityService leadActivityService;
    private final AuditEventService auditEventService;

    @Transactional
    public List<DuplicateCandidateResponse> rescan() {
        List<Lead> leads = leadRepository.findAll().stream()
                .filter(lead -> lead.getMergedIntoLeadId() == null)
                .sorted(Comparator.comparing(Lead::getId))
                .toList();
        Map<DuplicatePairKey, DuplicateMatch> existingMatches = new HashMap<>();
        for (DuplicateMatch match : duplicateMatchRepository.findAll()) {
            existingMatches.put(DuplicatePairKey.of(match.getLead(), match.getMatchedLead()), match);
        }
        List<DuplicateCandidateResponse> results = new ArrayList<>();
        Set<Long> suspectedLeadIds = new HashSet<>();
        for (int i = 0; i < leads.size(); i++) {
            for (int j = i + 1; j < leads.size(); j++) {
                Lead left = leads.get(i);
                Lead right = leads.get(j);
                int score = score(left, right);
                if (score < 70) {
                    continue;
                }
                DuplicatePairKey pairKey = DuplicatePairKey.of(left, right);
                Lead canonicalLead = pairKey.leadId().equals(left.getId()) ? left : right;
                Lead canonicalMatchedLead = pairKey.matchedLeadId().equals(right.getId()) ? right : left;
                DuplicateMatch match = existingMatches.remove(pairKey);
                if (match == null) {
                    match = new DuplicateMatch();
                }
                match.setLead(canonicalLead);
                match.setMatchedLead(canonicalMatchedLead);
                match.setMatchScore(score);
                match.setState(DuplicateState.SUSPECTED);
                match.setReason(buildReason(left, right));
                match.setReviewedBy(null);
                match.setReviewedAt(null);
                duplicateMatchRepository.save(match);
                suspectedLeadIds.add(canonicalLead.getId());
                suspectedLeadIds.add(canonicalMatchedLead.getId());
                results.add(DuplicateCandidateResponse.from(match));
            }
        }
        if (!existingMatches.isEmpty()) {
            duplicateMatchRepository.deleteAll(existingMatches.values());
        }
        reconcileLeadStates(leads, suspectedLeadIds);
        return results;
    }

    @Transactional(readOnly = true)
    public List<DuplicateCandidateResponse> list() {
        return duplicateMatchRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(DuplicateCandidateResponse::from)
                .toList();
    }

    @Transactional
    public DuplicateCandidateResponse updateState(Long id, DuplicateState state) {
        DuplicateMatch match = duplicateMatchRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Duplicate match not found"));
        match.setState(state);
        match.setReviewedBy(securityUtils.currentUserEntity());
        match.setReviewedAt(OffsetDateTime.now());
        return DuplicateCandidateResponse.from(match);
    }

    @Transactional
    public void merge(LeadMergeRequest request) {
        Lead source = leadRepository.findById(request.sourceLeadId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Source lead not found"));
        Lead target = leadRepository.findById(request.targetLeadId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Target lead not found"));
        if (source.getId().equals(target.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Source and target leads must be different");
        }
        source.setMergedIntoLeadId(target.getId());
        source.setDuplicateState(DuplicateState.MERGED);
        target.setDuplicateState(DuplicateState.CLEAR);
        for (LeadNote note : noteRepository.findByLeadIdOrderByCreatedAtDesc(source.getId())) {
            note.setLead(target);
        }
        for (LeadActivity activity : activityRepository.findByLeadIdOrderByCreatedAtDesc(source.getId())) {
            activity.setLead(target);
        }
        communicationRepository.findByLeadIdOrderByOccurredAtDesc(source.getId()).forEach(communication -> communication.setLead(target));
        attachmentRepository.findByLeadIdOrderByCreatedAtDesc(source.getId()).forEach(attachment -> attachment.setLead(target));
        documentRepository.findByLeadIdOrderByCreatedAtDesc(source.getId()).forEach(document -> document.setLead(target));
        for (LeadFollowup followup : followupRepository.findByLeadIdOrderByStepNumberAsc(source.getId())) {
            leadActivityService.log(target, securityUtils.currentUserEntity(), LeadActivityType.LEAD_MERGED,
                    "Merged follow-up " + followup.getStepNumber() + " history from source lead " + source.getCompanyName());
        }
        followupRepository.deleteAll(followupRepository.findByLeadIdOrderByStepNumberAsc(source.getId()));
        LeadMergeEvent event = new LeadMergeEvent();
        event.setSourceLeadId(source.getId());
        event.setTargetLead(target);
        event.setMergedBy(securityUtils.currentUserEntity());
        event.setSummary(request.summary() == null || request.summary().isBlank()
                ? "Merged " + source.getCompanyName() + " into " + target.getCompanyName()
                : request.summary().trim());
        mergeEventRepository.save(event);
        leadActivityService.log(target, securityUtils.currentUserEntity(), LeadActivityType.LEAD_MERGED, event.getSummary());
        auditEventService.log(securityUtils.currentUserEntity(), "LEAD_MERGED", "LEAD", target.getId(), event.getSummary(), null);
        leadRepository.delete(source);
    }

    private int score(Lead left, Lead right) {
        int score = 0;
        if (left.getEmail() != null && right.getEmail() != null && left.getEmail().equalsIgnoreCase(right.getEmail())) {
            score += 50;
        }
        if (left.getPhone() != null && right.getPhone() != null && left.getPhone().equalsIgnoreCase(right.getPhone())) {
            score += 30;
        }
        if (left.getCompanyName() != null && right.getCompanyName() != null) {
            int distance = levenshtein(left.getCompanyName().toLowerCase(), right.getCompanyName().toLowerCase());
            int maxLength = Math.max(left.getCompanyName().length(), right.getCompanyName().length());
            int similarity = maxLength == 0 ? 0 : Math.max(0, 100 - (distance * 100 / maxLength));
            score += similarity / 2;
        }
        return Math.min(score, 100);
    }

    private String buildReason(Lead left, Lead right) {
        List<String> reasons = new ArrayList<>();
        if (left.getEmail() != null && right.getEmail() != null && left.getEmail().equalsIgnoreCase(right.getEmail())) {
            reasons.add("matching email");
        }
        if (left.getPhone() != null && right.getPhone() != null && left.getPhone().equalsIgnoreCase(right.getPhone())) {
            reasons.add("matching phone");
        }
        if (left.getCompanyName() != null && right.getCompanyName() != null
                && levenshtein(left.getCompanyName().toLowerCase(), right.getCompanyName().toLowerCase()) <= 2) {
            reasons.add("similar company name");
        }
        return String.join(", ", reasons);
    }

    private int levenshtein(String left, String right) {
        int[][] dp = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= right.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[left.length()][right.length()];
    }

    private void reconcileLeadStates(List<Lead> leads, Set<Long> suspectedLeadIds) {
        for (Lead lead : leads) {
            lead.setDuplicateState(suspectedLeadIds.contains(lead.getId()) ? DuplicateState.SUSPECTED : DuplicateState.CLEAR);
        }
    }

    private record DuplicatePairKey(Long leadId, Long matchedLeadId) {

        private static DuplicatePairKey of(Lead left, Lead right) {
            long lowerId = Math.min(left.getId(), right.getId());
            long higherId = Math.max(left.getId(), right.getId());
            return new DuplicatePairKey(lowerId, higherId);
        }
    }
}
