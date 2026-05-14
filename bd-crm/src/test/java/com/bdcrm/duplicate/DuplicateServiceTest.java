package com.bdcrm.duplicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bdcrm.attachment.AttachmentRecordRepository;
import com.bdcrm.attachment.DocumentRecordRepository;
import com.bdcrm.audit.AuditEventService;
import com.bdcrm.auth.SecurityUtils;
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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DuplicateServiceTest {

    private DuplicateMatchRepository duplicateMatchRepository;
    private LeadMergeEventRepository mergeEventRepository;
    private LeadRepository leadRepository;
    private LeadNoteRepository noteRepository;
    private LeadActivityRepository activityRepository;
    private LeadFollowupRepository followupRepository;
    private LeadCommunicationRepository communicationRepository;
    private AttachmentRecordRepository attachmentRepository;
    private DocumentRecordRepository documentRepository;
    private SecurityUtils securityUtils;
    private LeadActivityService leadActivityService;
    private AuditEventService auditEventService;
    private DuplicateService duplicateService;

    @BeforeEach
    void setUp() {
        duplicateMatchRepository = mock(DuplicateMatchRepository.class);
        mergeEventRepository = mock(LeadMergeEventRepository.class);
        leadRepository = mock(LeadRepository.class);
        noteRepository = mock(LeadNoteRepository.class);
        activityRepository = mock(LeadActivityRepository.class);
        followupRepository = mock(LeadFollowupRepository.class);
        communicationRepository = mock(LeadCommunicationRepository.class);
        attachmentRepository = mock(AttachmentRecordRepository.class);
        documentRepository = mock(DocumentRecordRepository.class);
        securityUtils = mock(SecurityUtils.class);
        leadActivityService = mock(LeadActivityService.class);
        auditEventService = mock(AuditEventService.class);

        duplicateService = new DuplicateService(
                duplicateMatchRepository,
                mergeEventRepository,
                leadRepository,
                noteRepository,
                activityRepository,
                followupRepository,
                communicationRepository,
                attachmentRepository,
                documentRepository,
                securityUtils,
                leadActivityService,
                auditEventService);
    }

    private Lead mockLead(Long id, String companyName, String email, String phone, String contactName, String source) {
        Lead lead = mock(Lead.class);
        when(lead.getId()).thenReturn(id);
        when(lead.getCompanyName()).thenReturn(companyName);
        when(lead.getEmail()).thenReturn(email);
        when(lead.getPhone()).thenReturn(phone);
        when(lead.getContactName()).thenReturn(contactName);
        when(lead.getSource()).thenReturn(source);
        when(lead.getMergedIntoLeadId()).thenReturn(null);
        when(lead.getDuplicateState()).thenReturn(DuplicateState.CLEAR);
        return lead;
    }

    @Test
    void scoreExactEmailMatchGives40Points() {
        Lead left = mockLead(1L, "Acme Corp", "test@example.com", null, "John Doe", null);
        Lead right = mockLead(2L, "Different Company", "test@example.com", null, "Jane Doe", null);

        when(leadRepository.findAll()).thenReturn(List.of(left, right));
        when(duplicateMatchRepository.findByLeadIdAndMatchedLeadId(1L, 2L)).thenReturn(java.util.Optional.empty());
        when(duplicateMatchRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<DuplicateCandidateResponse> results = duplicateService.rescan();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).matchScore()).isEqualTo(40);
    }

    @Test
    void scoreNormalizedPhoneMatchGives30Points() {
        // Phone formats differ but normalize to same digits
        Lead left = mockLead(1L, "Acme Corp", null, "555-1234", "John Doe", null);
        Lead right = mockLead(2L, "Different Company", null, "(555) 1234", "Jane Doe", null);

        when(leadRepository.findAll()).thenReturn(List.of(left, right));
        when(duplicateMatchRepository.findByLeadIdAndMatchedLeadId(1L, 2L)).thenReturn(java.util.Optional.empty());
        when(duplicateMatchRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<DuplicateCandidateResponse> results = duplicateService.rescan();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).matchScore()).isEqualTo(30);
    }

    @Test
    void scoreFuzzyCompanyNameGivesPartialPoints() {
        // "Acme Corp" vs "Acme Corporation" - very similar, Levenshtein distance = 9
        Lead left = mockLead(1L, "Acme Corp", null, null, "John Doe", null);
        Lead right = mockLead(2L, "Acme Corporation", null, null, "Jane Doe", null);

        when(leadRepository.findAll()).thenReturn(List.of(left, right));
        when(duplicateMatchRepository.findByLeadIdAndMatchedLeadId(1L, 2L)).thenReturn(java.util.Optional.empty());
        when(duplicateMatchRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<DuplicateCandidateResponse> results = duplicateService.rescan();

        // Distance 9, similarity ~70%, partial score
        assertThat(results).hasSize(1);
        assertThat(results.get(0).matchScore()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void scoreExactCompanyNameMatchGives30Points() {
        // Same company name gives full 30 points
        Lead left = mockLead(1L, "Acme Corp", null, null, "John Doe", null);
        Lead right = mockLead(2L, "Acme Corp", null, null, "Jane Doe", null);

        when(leadRepository.findAll()).thenReturn(List.of(left, right));
        when(duplicateMatchRepository.findByLeadIdAndMatchedLeadId(1L, 2L)).thenReturn(java.util.Optional.empty());
        when(duplicateMatchRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<DuplicateCandidateResponse> results = duplicateService.rescan();

        // Same company = 30, no email/phone
        assertThat(results).hasSize(1);
        assertThat(results.get(0).matchScore()).isEqualTo(30);
    }

    @Test
    void scoreFuzzyContactNameGivesPartialPoints() {
        // "John Smith" vs "John Smyth" - very similar
        Lead left = mockLead(1L, "Acme Corp", null, null, "John Smith", null);
        Lead right = mockLead(2L, "Acme Corp", null, null, "John Smyth", null);

        when(leadRepository.findAll()).thenReturn(List.of(left, right));
        when(duplicateMatchRepository.findByLeadIdAndMatchedLeadId(1L, 2L)).thenReturn(java.util.Optional.empty());
        when(duplicateMatchRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<DuplicateCandidateResponse> results = duplicateService.rescan();

        // Same company = 30, fuzzy name partial
        assertThat(results).hasSize(1);
        assertThat(results.get(0).matchScore()).isGreaterThanOrEqualTo(30);
    }

    @Test
    void scoreMatchesAllFactorsGivesMax100() {
        // Same email (40) + same phone (30) + exact company (30) = 100
        Lead left = mockLead(1L, "Acme Corp", "test@example.com", "555-1234", "John Doe", "Website");
        Lead right = mockLead(2L, "Acme Corp", "test@example.com", "555-1234", "John Doe", "Website");

        when(leadRepository.findAll()).thenReturn(List.of(left, right));
        when(duplicateMatchRepository.findByLeadIdAndMatchedLeadId(1L, 2L)).thenReturn(java.util.Optional.empty());
        when(duplicateMatchRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<DuplicateCandidateResponse> results = duplicateService.rescan();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).matchScore()).isEqualTo(100);
    }

    @Test
    void scoreBelowThresholdNotReported() {
        Lead left = mockLead(1L, "Acme Corp", "different1@example.com", "555-1111", "John Doe", "Website");
        Lead right = mockLead(2L, "Totally Different Company", "different2@example.com", "555-2222", "Jane Smith", "Referral");

        when(leadRepository.findAll()).thenReturn(List.of(left, right));

        List<DuplicateCandidateResponse> results = duplicateService.rescan();

        // Very different - score below 65 threshold
        assertThat(results).isEmpty();
    }

    @Test
    void scoreSameSourceGives10Points() {
        Lead left = mockLead(1L, "Acme Corp", null, null, "John Doe", "Website");
        Lead right = mockLead(2L, "Different Company", null, null, "Jane Doe", "Website");

        when(leadRepository.findAll()).thenReturn(List.of(left, right));
        when(duplicateMatchRepository.findByLeadIdAndMatchedLeadId(1L, 2L)).thenReturn(java.util.Optional.empty());
        when(duplicateMatchRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<DuplicateCandidateResponse> results = duplicateService.rescan();

        // Same source = 10
        assertThat(results).hasSize(1);
        assertThat(results.get(0).matchScore()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void listReturnsAllMatches() {
        DuplicateMatch match = new DuplicateMatch();
        match.setId(1L);
        match.setMatchScore(85);
        when(duplicateMatchRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(match));

        List<DuplicateCandidateResponse> results = duplicateService.list();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).matchScore()).isEqualTo(85);
    }

    @Test
    void updateStateChangesState() {
        DuplicateMatch match = new DuplicateMatch();
        match.setId(1L);
        match.setState(DuplicateState.SUSPECTED);

        when(duplicateMatchRepository.findById(1L)).thenReturn(java.util.Optional.of(match));
        when(securityUtils.currentUserEntity()).thenReturn(null);

        DuplicateCandidateResponse result = duplicateService.updateState(1L, DuplicateState.REVIEWED);

        assertThat(result.state()).isEqualTo(DuplicateState.REVIEWED);
    }

    @Test
    void buildReasonIncludesNormalizedPhoneMatch() {
        Lead left = mockLead(1L, "Acme Corp", null, "555-1234", "John Doe", null);
        Lead right = mockLead(2L, "Acme Corp", null, "(555) 1234", "Jane Doe", null);

        when(leadRepository.findAll()).thenReturn(List.of(left, right));
        when(duplicateMatchRepository.findByLeadIdAndMatchedLeadId(1L, 2L)).thenReturn(java.util.Optional.empty());
        when(duplicateMatchRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<DuplicateCandidateResponse> results = duplicateService.rescan();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).reason()).contains("matching phone");
    }

    @Test
    void buildReasonIncludesFuzzyCompanyMatch() {
        Lead left = mockLead(1L, "Acme Corp", null, null, "John Doe", null);
        Lead right = mockLead(2L, "Acmex Corp", null, null, "Jane Doe", null);

        when(leadRepository.findAll()).thenReturn(List.of(left, right));
        when(duplicateMatchRepository.findByLeadIdAndMatchedLeadId(1L, 2L)).thenReturn(java.util.Optional.empty());
        when(duplicateMatchRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<DuplicateCandidateResponse> results = duplicateService.rescan();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).reason()).contains("similar company name");
    }

    @Test
    void buildReasonIncludesFuzzyContactMatch() {
        Lead left = mockLead(1L, "Acme Corp", null, null, "John Smith", null);
        Lead right = mockLead(2L, "Acme Corp", null, null, "John Smyth", null);

        when(leadRepository.findAll()).thenReturn(List.of(left, right));
        when(duplicateMatchRepository.findByLeadIdAndMatchedLeadId(1L, 2L)).thenReturn(java.util.Optional.empty());
        when(duplicateMatchRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<DuplicateCandidateResponse> results = duplicateService.rescan();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).reason()).contains("similar contact name");
    }

    @Test
    void scorePhoneWithSpacesAndDashesMatches() {
        // "123-456-7890" vs "123 456 7890" should match after normalization
        Lead left = mockLead(1L, "Acme Corp", null, "123-456-7890", "John Doe", null);
        Lead right = mockLead(2L, "Different Company", null, "123 456 7890", "Jane Doe", null);

        when(leadRepository.findAll()).thenReturn(List.of(left, right));
        when(duplicateMatchRepository.findByLeadIdAndMatchedLeadId(1L, 2L)).thenReturn(java.util.Optional.empty());
        when(duplicateMatchRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<DuplicateCandidateResponse> results = duplicateService.rescan();

        // Both normalize to "1234567890"
        assertThat(results).hasSize(1);
        assertThat(results.get(0).matchScore()).isEqualTo(30);
    }
}