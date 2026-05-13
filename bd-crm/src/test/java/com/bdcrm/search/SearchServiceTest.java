package com.bdcrm.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bdcrm.attachment.AttachmentRecordRepository;
import com.bdcrm.followup.FollowupOutcome;
import com.bdcrm.followup.FollowupStatus;
import com.bdcrm.followup.LeadFollowup;
import com.bdcrm.followup.LeadFollowupRepository;
import com.bdcrm.lead.Lead;
import com.bdcrm.lead.LeadActivity;
import com.bdcrm.lead.LeadActivityRepository;
import com.bdcrm.lead.LeadNote;
import com.bdcrm.lead.LeadNoteRepository;
import com.bdcrm.lead.LeadRepository;
import com.bdcrm.lead.LeadStatus;
import com.bdcrm.user.User;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class SearchServiceTest {

    private LeadRepository leadRepository;
    private LeadNoteRepository noteRepository;
    private LeadActivityRepository activityRepository;
    private LeadFollowupRepository followupRepository;
    private AttachmentRecordRepository attachmentRepository;
    private SearchService searchService;

    @BeforeEach
    void setUp() {
        leadRepository = mock(LeadRepository.class);
        noteRepository = mock(LeadNoteRepository.class);
        activityRepository = mock(LeadActivityRepository.class);
        followupRepository = mock(LeadFollowupRepository.class);
        attachmentRepository = mock(AttachmentRecordRepository.class);
        searchService = new SearchService(
                leadRepository, noteRepository, activityRepository, followupRepository, attachmentRepository);
    }

    private User mockUser(Long id, String fullName) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getFullName()).thenReturn(fullName);
        return user;
    }

    private Lead mockLead(Long id, String companyName, String contactName, User assignedUser) {
        Lead lead = mock(Lead.class);
        when(lead.getId()).thenReturn(id);
        when(lead.getCompanyName()).thenReturn(companyName);
        when(lead.getContactName()).thenReturn(contactName);
        when(lead.getAssignedUser()).thenReturn(assignedUser);
        when(lead.getMergedIntoLeadId()).thenReturn(null);
        when(lead.getStatus()).thenReturn(LeadStatus.NEW);
        return lead;
    }

    private LeadNote mockNote(Long id, String body, Lead lead, User author) {
        LeadNote note = mock(LeadNote.class);
        when(note.getId()).thenReturn(id);
        when(note.getBody()).thenReturn(body);
        when(note.getLead()).thenReturn(lead);
        when(note.getAuthor()).thenReturn(author);
        return note;
    }

    private LeadActivity mockActivity(Long id, String description, Lead lead) {
        LeadActivity activity = mock(LeadActivity.class);
        when(activity.getId()).thenReturn(id);
        when(activity.getDescription()).thenReturn(description);
        when(activity.getLead()).thenReturn(lead);
        when(activity.getType()).thenReturn(com.bdcrm.lead.LeadActivityType.EMAIL_SENT);
        return activity;
    }

    private LeadFollowup mockFollowup(Long id, String notes, FollowupOutcome outcome, Lead lead) {
        LeadFollowup followup = mock(LeadFollowup.class);
        when(followup.getId()).thenReturn(id);
        when(followup.getNotes()).thenReturn(notes);
        when(followup.getOutcome()).thenReturn(outcome);
        when(followup.getLead()).thenReturn(lead);
        when(followup.getStepNumber()).thenReturn(1);
        when(followup.getStatus()).thenReturn(FollowupStatus.COMPLETED);
        return followup;
    }

    private AttachmentRecord mockAttachment(Long id, String fileName, Lead lead) {
        AttachmentRecord attachment = mock(AttachmentRecord.class);
        when(attachment.getId()).thenReturn(id);
        when(attachment.getOriginalFileName()).thenReturn(fileName);
        when(attachment.getLead()).thenReturn(lead);
        when(attachment.getContentType()).thenReturn("application/pdf");
        return attachment;
    }

    @SuppressWarnings("unchecked")
    private Page<Lead> emptyPage() {
        return new PageImpl<>(List.of());
    }

    @SuppressWarnings("unchecked")
    private Page<Lead> pageOf(Lead... leads) {
        return new PageImpl<>(List.of(leads));
    }

    @Test
    void returnsEmptyResultsWhenNoData() {
        when(leadRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage());
        when(noteRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(activityRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(followupRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(attachmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        GlobalSearchResponse result = searchService.search("test", null, null, null, null, null, null, null);

        assertThat(result.leads()).isEmpty();
        assertThat(result.notes()).isEmpty();
        assertThat(result.activities()).isEmpty();
        assertThat(result.followups()).isEmpty();
        assertThat(result.attachments()).isEmpty();
    }

    @Test
    void filtersLeadsByQuery() {
        User user = mockUser(1L, "John Doe");
        Lead lead1 = mockLead(1L, "Acme Corp", "John Smith", user);
        Lead lead2 = mockLead(2L, "Beta Inc", "Jane Doe", user);

        when(leadRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageOf(lead1, lead2));
        when(noteRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(activityRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(followupRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(attachmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        GlobalSearchResponse result = searchService.search("acme", null, null, null, null, null, null, null);

        assertThat(result.leads()).hasSize(2);
    }

    @Test
    void excludesMergedLeads() {
        User user = mockUser(1L, "John Doe");
        Lead activeLead = mockLead(1L, "Acme Corp", "John Smith", user);
        Lead mergedLead = mockLead(2L, "Merged Co", "Merged Person", user);
        when(mergedLead.getMergedIntoLeadId()).thenReturn(99L);

        when(leadRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageOf(activeLead));
        when(noteRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(activityRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(followupRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(attachmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        GlobalSearchResponse result = searchService.search("", null, null, null, null, null, null, null);

        assertThat(result.leads()).hasSize(1);
        assertThat(result.leads().get(0).title()).isEqualTo("Acme Corp");
    }

    @Test
    void limitsResultsToTen() {
        User user = mockUser(1L, "John Doe");
        List<Lead> manyLeads = java.util.stream.IntStream.rangeClosed(1, 25)
                .mapToObj(i -> mockLead((long) i, "Company " + i, "Contact " + i, user))
                .toList();

        when(leadRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(manyLeads));
        when(noteRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(activityRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(followupRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(attachmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        GlobalSearchResponse result = searchService.search("", null, null, null, null, null, null, null);

        assertThat(result.leads()).hasSize(10);
    }

    @Test
    void filtersByOwner() {
        User user1 = mockUser(1L, "John Doe");
        User user2 = mockUser(2L, "Jane Smith");
        Lead lead1 = mockLead(1L, "Acme Corp", "John Smith", user1);
        Lead lead2 = mockLead(2L, "Beta Inc", "Jane Doe", user2);

        when(leadRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageOf(lead1));
        when(noteRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(activityRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(followupRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(attachmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        GlobalSearchResponse result = searchService.search("", 1L, null, null, null, null, null, null);

        assertThat(result.leads()).hasSize(1);
        assertThat(result.leads().get(0).title()).isEqualTo("Acme Corp");
    }

    @Test
    void filtersByStatus() {
        User user = mockUser(1L, "John Doe");
        Lead newLead = mockLead(1L, "Acme Corp", "John Smith", user);
        when(newLead.getStatus()).thenReturn(LeadStatus.NEW);
        Lead wonLead = mockLead(2L, "Beta Inc", "Jane Doe", user);
        when(wonLead.getStatus()).thenReturn(LeadStatus.WON);

        when(leadRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageOf(newLead));
        when(noteRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(activityRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(followupRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(attachmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        GlobalSearchResponse result = searchService.search("", null, LeadStatus.NEW, null, null, null, null, null);

        assertThat(result.leads()).hasSize(1);
        assertThat(result.leads().get(0).title()).isEqualTo("Acme Corp");
    }

    @Test
    void filtersNotesByQuery() {
        User user = mockUser(1L, "John Doe");
        Lead lead = mockLead(1L, "Acme Corp", "John Smith", user);
        LeadNote note1 = mockNote(1L, "Important note about project", lead, user);
        LeadNote note2 = mockNote(2L, "Another random note", lead, user);

        when(leadRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(noteRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(note1, note2)));
        when(activityRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(followupRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(attachmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        GlobalSearchResponse result = searchService.search("project", null, null, null, null, null, null, null);

        assertThat(result.notes()).hasSize(2);
    }

    @Test
    void filtersFollowupsByOutcome() {
        User user = mockUser(1L, "John Doe");
        Lead lead = mockLead(1L, "Acme Corp", "John Smith", user);
        LeadFollowup followup1 = mockFollowup(1L, "Notes here", FollowupOutcome.INTERESTED, lead);
        LeadFollowup followup2 = mockFollowup(2L, "Other notes", FollowupOutcome.NOT_INTERESTED, lead);

        when(leadRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(noteRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(activityRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(followupRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(followup1, followup2)));
        when(attachmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        GlobalSearchResponse result = searchService.search("", null, null, null, null, "INTERESTED", null, null);

        assertThat(result.followups()).hasSize(2);
    }

    @Test
    void filtersByDateRange() {
        User user = mockUser(1L, "John Doe");
        Lead lead = mockLead(1L, "Acme Corp", "John Smith", user);

        when(leadRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageOf(lead));
        when(noteRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(activityRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(followupRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(attachmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        LocalDate dateFrom = LocalDate.now().minusDays(7);
        LocalDate dateTo = LocalDate.now();

        GlobalSearchResponse result = searchService.search("", null, null, null, null, null, dateFrom, dateTo);

        assertThat(result.leads()).hasSize(1);
    }
}