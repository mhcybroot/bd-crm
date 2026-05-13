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
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DuplicateService {

    private static final Pattern PHONE_DIGITS = Pattern.compile("\\D");
    private static final int MATCH_THRESHOLD = 65;
    private static final int EMAIL_MATCH_WEIGHT = 40;
    private static final int PHONE_MATCH_WEIGHT = 30;
    private static final int COMPANY_MATCH_WEIGHT = 30;
    private static final int CONTACT_MATCH_WEIGHT = 15;
    private static final int SOURCE_MATCH_WEIGHT = 10;
    private static final int FUZZY_THRESHOLD = 5;

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

    public DuplicateService(
            DuplicateMatchRepository duplicateMatchRepository,
            LeadMergeEventRepository mergeEventRepository,
            LeadRepository leadRepository,
            LeadNoteRepository noteRepository,
            LeadActivityRepository activityRepository,
            LeadFollowupRepository followupRepository,
            LeadCommunicationRepository communicationRepository,
            AttachmentRecordRepository attachmentRepository,
            DocumentRecordRepository documentRepository,
            SecurityUtils securityUtils,
            LeadActivityService leadActivityService,
            AuditEventService auditEventService) {
        this.duplicateMatchRepository = duplicateMatchRepository;
        this.mergeEventRepository = mergeEventRepository;
        this.leadRepository = leadRepository;
        this.noteRepository = noteRepository;
        this.activityRepository = activityRepository;
        this.followupRepository = followupRepository;
        this.communicationRepository = communicationRepository;
        this.attachmentRepository = attachmentRepository;
        this.documentRepository = documentRepository;
        this.securityUtils = securityUtils;
        this.leadActivityService = leadActivityService;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public List<DuplicateCandidateResponse> rescan() {
        List<Lead> leads = leadRepository.findAll().stream()
                .filter(lead -> lead.getMergedIntoLeadId() == null)
                .toList();

        log.info("Starting duplicate scan for {} leads", leads.size());
        List<DuplicateCandidateResponse> results = new ArrayList<>();

        for (int i = 0; i < leads.size(); i++) {
            for (int j = i + 1; j < leads.size(); j++) {
                Lead left = leads.get(i);
                Lead right = leads.get(j);

                int score = score(left, right);
                if (score < MATCH_THRESHOLD) {
                    continue;
                }

                DuplicateMatch match = duplicateMatchRepository
                        .findByLeadIdAndMatchedLeadId(left.getId(), right.getId())
                        .orElseGet(DuplicateMatch::new);

                match.setLead(left);
                match.setMatchedLead(right);
                match.setMatchScore(score);
                match.setState(DuplicateState.SUSPECTED);
                match.setReason(buildReason(left, right, score));

                duplicateMatchRepository.save(match);

                left.setDuplicateState(DuplicateState.SUSPECTED);
                right.setDuplicateState(DuplicateState.SUSPECTED);

                results.add(DuplicateCandidateResponse.from(match));
            }
        }

        log.info("Duplicate scan complete. Found {} potential duplicates", results.size());
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
        DuplicateMatch match = duplicateMatchRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Duplicate match not found"));
        match.setState(state);
        match.setReviewedBy(securityUtils.currentUserEntity());
        match.setReviewedAt(OffsetDateTime.now());
        return DuplicateCandidateResponse.from(match);
    }

    @Transactional
    public void merge(LeadMergeRequest request) {
        Lead source = leadRepository.findById(request.sourceLeadId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Source lead not found"));
        Lead target = leadRepository.findById(request.targetLeadId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Target lead not found"));

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
        communicationRepository.findByLeadIdOrderByOccurredAtDesc(source.getId())
                .forEach(communication -> communication.setLead(target));
        attachmentRepository.findByLeadIdOrderByCreatedAtDesc(source.getId())
                .forEach(attachment -> attachment.setLead(target));
        documentRepository.findByLeadIdOrderByCreatedAtDesc(source.getId())
                .forEach(document -> document.setLead(target));

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

    int score(Lead left, Lead right) {
        int score = 0;

        // Email exact match (normalized)
        if (normalizedEmailMatch(left.getEmail(), right.getEmail())) {
            score += EMAIL_MATCH_WEIGHT;
        }

        // Phone normalized match (digits only comparison)
        if (normalizedPhoneMatch(left.getPhone(), right.getPhone())) {
            score += PHONE_MATCH_WEIGHT;
        }

        // Company name fuzzy match with Levenshtein distance
        if (left.getCompanyName() != null && right.getCompanyName() != null) {
            int companyScore = fuzzyCompanyScore(left.getCompanyName(), right.getCompanyName());
            score += companyScore;
        }

        // Contact name fuzzy match
        if (left.getContactName() != null && right.getContactName() != null) {
            int contactScore = fuzzyNameScore(left.getContactName(), right.getContactName());
            score += contactScore;
        }

        // Source similarity
        if (left.getSource() != null && right.getSource() != null) {
            if (left.getSource().equalsIgnoreCase(right.getSource())) {
                score += SOURCE_MATCH_WEIGHT;
            }
        }

        return Math.min(score, 100);
    }

    private boolean normalizedEmailMatch(String left, String right) {
        if (left == null || right == null) return false;
        return normalizeEmail(left).equals(normalizeEmail(right));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private boolean normalizedPhoneMatch(String left, String right) {
        if (left == null || right == null) return false;
        return digitsOnly(left).equals(digitsOnly(right));
    }

    private String digitsOnly(String phone) {
        return PHONE_DIGITS.matcher(phone).replaceAll("");
    }

    private int fuzzyCompanyScore(String left, String right) {
        String normalizedLeft = normalizeForComparison(left);
        String normalizedRight = normalizeForComparison(right);

        // Exact match after normalization
        if (normalizedLeft.equals(normalizedRight)) {
            return COMPANY_MATCH_WEIGHT;
        }

        // Levenshtein-based similarity
        int distance = levenshtein(normalizedLeft, normalizedRight);
        int maxLength = Math.max(normalizedLeft.length(), normalizedRight.length());

        if (maxLength == 0) return 0;

        int similarity = Math.max(0, 100 - (distance * 100 / maxLength));

        // If similarity is above threshold, give full weight, otherwise partial
        if (similarity >= 85) {
            return COMPANY_MATCH_WEIGHT;
        } else if (similarity >= 70) {
            return COMPANY_MATCH_WEIGHT / 2;
        } else if (distance <= FUZZY_THRESHOLD) {
            return COMPANY_MATCH_WEIGHT / 3;
        }

        return 0;
    }

    private int fuzzyNameScore(String left, String right) {
        String normalizedLeft = normalizeForComparison(left);
        String normalizedRight = normalizeForComparison(right);

        if (normalizedLeft.equals(normalizedRight)) {
            return CONTACT_MATCH_WEIGHT;
        }

        int distance = levenshtein(normalizedLeft, normalizedRight);
        int maxLength = Math.max(normalizedLeft.length(), normalizedRight.length());

        if (maxLength == 0) return 0;

        int similarity = Math.max(0, 100 - (distance * 100 / maxLength));

        if (similarity >= 80) {
            return CONTACT_MATCH_WEIGHT;
        } else if (distance <= 2) {
            return CONTACT_MATCH_WEIGHT / 2;
        }

        return 0;
    }

    private String normalizeForComparison(String input) {
        return input.trim().toLowerCase();
    }

    String buildReason(Lead left, Lead right, int score) {
        List<String> reasons = new ArrayList<>();

        if (normalizedEmailMatch(left.getEmail(), right.getEmail())) {
            reasons.add("matching email");
        }

        if (normalizedPhoneMatch(left.getPhone(), right.getPhone())) {
            reasons.add("matching phone (normalized)");
        }

        if (left.getCompanyName() != null && right.getCompanyName() != null) {
            int distance = levenshtein(normalizeForComparison(left.getCompanyName()),
                    normalizeForComparison(right.getCompanyName()));
            if (distance <= FUZZY_THRESHOLD) {
                reasons.add("similar company name (distance=" + distance + ")");
            }
        }

        if (left.getContactName() != null && right.getContactName() != null) {
            int distance = levenshtein(normalizeForComparison(left.getContactName()),
                    normalizeForComparison(right.getContactName()));
            if (distance <= 2) {
                reasons.add("similar contact name (distance=" + distance + ")");
            }
        }

        if (reasons.isEmpty()) {
            reasons.add("overall similarity score: " + score);
        }

        return String.join(", ", reasons);
    }

    int levenshtein(String left, String right) {
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
}