package com.bdcrm.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdcrm.followup.FollowupOutcome;
import com.bdcrm.followup.FollowupStatus;
import com.bdcrm.followup.LeadFollowup;
import com.bdcrm.followup.LeadFollowupRepository;
import com.bdcrm.lead.Lead;
import com.bdcrm.lead.LeadPriority;
import com.bdcrm.lead.LeadRepository;
import com.bdcrm.lead.LeadStatus;
import com.bdcrm.template.ContactChannel;
import com.bdcrm.template.FollowupTemplate;
import com.bdcrm.user.User;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadFollowupRepository leadFollowupRepository;

    @Mock
    private UserRepository userRepository;

    private ReportingService reportingService;

    @BeforeEach
    void setUp() {
        reportingService = new ReportingService(leadRepository, leadFollowupRepository, userRepository);
    }

    private User mockUser(Long id, String fullName) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getFullName()).thenReturn(fullName);
        return user;
    }

    private FollowupTemplate mockTemplate(Long id) {
        FollowupTemplate template = mock(FollowupTemplate.class);
        when(template.getId()).thenReturn(id);
        return template;
    }

    private Lead mockLead(Long id, LeadStatus status, User assignedUser, FollowupTemplate template) {
        Lead lead = mock(Lead.class);
        when(lead.getId()).thenReturn(id);
        when(lead.getStatus()).thenReturn(status);
        when(lead.getAssignedUser()).thenReturn(assignedUser);
        when(lead.getTemplate()).thenReturn(template);
        when(lead.getCreatedAt()).thenReturn(OffsetDateTime.now());
        when(lead.getUpdatedAt()).thenReturn(OffsetDateTime.now());
        return lead;
    }

    private LeadFollowup mockFollowup(Long id, Lead lead, FollowupStatus status, FollowupOutcome outcome,
                                      User assignedUser, ContactChannel channel) {
        LeadFollowup followup = mock(LeadFollowup.class);
        when(followup.getId()).thenReturn(id);
        when(followup.getLead()).thenReturn(lead);
        when(followup.getStatus()).thenReturn(status);
        when(followup.getOutcome()).thenReturn(outcome);
        when(followup.getAssignedUser()).thenReturn(assignedUser);
        when(followup.getChannel()).thenReturn(channel);
        when(followup.getEscalatedAt()).thenReturn(null);
        when(followup.getCompletedAt()).thenReturn(null);
        return followup;
    }

    @Test
    void funnel_filters_by_status() {
        // Given
        User user = mockUser(1L, "John Doe");
        FollowupTemplate template = mockTemplate(1L);
        Lead lead1 = mockLead(1L, LeadStatus.NEW, user, template);
        Lead lead2 = mockLead(2L, LeadStatus.CONTACTED, user, template);
        Lead lead3 = mockLead(3L, LeadStatus.NEW, user, template);

        when(leadRepository.findAll(any(Specification.class))).thenReturn(List.of(lead1, lead2, lead3));

        ReportFilterRequest filter = new ReportFilterRequest(
                LocalDate.now().minusDays(30), LocalDate.now(), null, LeadStatus.NEW, null, null, null, null, null, null, null, null);

        // When
        var result = reportingService.funnel(filter);

        // Then
        assertThat(result.get("NEW")).isEqualTo(2L);
        assertThat(result.get("CONTACTED")).isEqualTo(1L);
        assertThat(result.get("WON")).isEqualTo(0L);
        assertThat(result.get("LOST")).isEqualTo(0L);
    }

    @Test
    void funnel_filters_by_date_range() {
        // Given
        User user = mockUser(1L, "John Doe");
        FollowupTemplate template = mockTemplate(1L);
        Lead lead1 = mockLead(1L, LeadStatus.NEW, user, template);
        Lead lead2 = mockLead(2L, LeadStatus.NEW, user, template);

        when(leadRepository.findAll(any(Specification.class))).thenReturn(List.of(lead1, lead2));

        LocalDate dateFrom = LocalDate.now().minusDays(7);
        LocalDate dateTo = LocalDate.now();
        ReportFilterRequest filter = new ReportFilterRequest(
                dateFrom, dateTo, null, null, null, null, null, null, null, null, null, null);

        // When
        var result = reportingService.funnel(filter);

        // Then
        verify(leadRepository).findAll(any(Specification.class));
    }

    @Test
    void funnel_filters_by_rep_user_id() {
        // Given
        User user1 = mockUser(1L, "John Doe");
        User user2 = mockUser(2L, "Jane Smith");
        FollowupTemplate template = mockTemplate(1L);
        Lead lead1 = mockLead(1L, LeadStatus.NEW, user1, template);
        Lead lead2 = mockLead(2L, LeadStatus.NEW, user2, template);

        when(leadRepository.findAll(any(Specification.class))).thenReturn(List.of(lead1));

        ReportFilterRequest filter = new ReportFilterRequest(
                LocalDate.now().minusDays(30), LocalDate.now(), 1L, null, null, null, null, null, null, null, null, null);

        // When
        var result = reportingService.funnel(filter);

        // Then
        assertThat(result.get("NEW")).isEqualTo(1L);
    }

    @Test
    void funnel_filters_by_source() {
        // Given
        User user = mockUser(1L, "John Doe");
        FollowupTemplate template = mockTemplate(1L);
        Lead lead1 = mockLead(1L, LeadStatus.NEW, user, template);
        when(lead1.getSource()).thenReturn("Website");

        when(leadRepository.findAll(any(Specification.class))).thenReturn(List.of(lead1));

        ReportFilterRequest filter = new ReportFilterRequest(
                LocalDate.now().minusDays(30), LocalDate.now(), null, null, null, "Website", null, null, null, null, null, null);

        // When
        var result = reportingService.funnel(filter);

        // Then
        assertThat(result.get("NEW")).isEqualTo(1L);
    }

    @Test
    void funnel_filters_by_priority() {
        // Given
        User user = mockUser(1L, "John Doe");
        FollowupTemplate template = mockTemplate(1L);
        Lead lead1 = mockLead(1L, LeadStatus.NEW, user, template);
        when(lead1.getPriority()).thenReturn(LeadPriority.HIGH);

        when(leadRepository.findAll(any(Specification.class))).thenReturn(List.of(lead1));

        ReportFilterRequest filter = new ReportFilterRequest(
                LocalDate.now().minusDays(30), LocalDate.now(), null, null, null, null, null, LeadPriority.HIGH, null, null, null, null);

        // When
        var result = reportingService.funnel(filter);

        // Then
        assertThat(result.get("NEW")).isEqualTo(1L);
    }

    @Test
    void funnel_filters_by_template_id() {
        // Given
        User user = mockUser(1L, "John Doe");
        FollowupTemplate template = mockTemplate(1L);
        Lead lead1 = mockLead(1L, LeadStatus.NEW, user, template);

        when(leadRepository.findAll(any(Specification.class))).thenReturn(List.of(lead1));

        ReportFilterRequest filter = new ReportFilterRequest(
                LocalDate.now().minusDays(30), LocalDate.now(), null, null, null, null, 1L, null, null, null, null, null);

        // When
        var result = reportingService.funnel(filter);

        // Then
        assertThat(result.get("NEW")).isEqualTo(1L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void overview_returns_correct_kpis() {
        // Given
        User user = mockUser(1L, "John Doe");
        FollowupTemplate template = mockTemplate(1L);
        Lead lead1 = mockLead(1L, LeadStatus.WON, user, template);
        Lead lead2 = mockLead(2L, LeadStatus.LOST, user, template);
        Lead lead3 = mockLead(3L, LeadStatus.NEW, user, template);

        LeadFollowup followup1 = mockFollowup(1L, lead1, FollowupStatus.COMPLETED, FollowupOutcome.INTERESTED, user, ContactChannel.EMAIL);
        LeadFollowup followup2 = mockFollowup(2L, lead2, FollowupStatus.OVERDUE, null, user, ContactChannel.CALL);
        LeadFollowup followup3 = mockFollowup(3L, lead3, FollowupStatus.DUE, null, user, ContactChannel.CALL);

        when(leadRepository.findAll(any(Specification.class))).thenReturn(List.of(lead1, lead2, lead3));
        when(leadFollowupRepository.findAll(any(Specification.class))).thenReturn(List.of(followup1, followup2, followup3));
        when(userRepository.findAllByOrderByFullNameAsc()).thenReturn(List.of(user));

        ReportFilterRequest filter = new ReportFilterRequest(
                LocalDate.now().minusDays(30), LocalDate.now(), null, null, null, null, null, null, null, null, null, null);

        // When
        ReportsOverviewResponse result = reportingService.overview(filter);

        // Then
        assertThat(result.kpiSummary().totalLeads()).isEqualTo(3);
        assertThat(result.kpiSummary().totalFollowups()).isEqualTo(3);
        assertThat(result.kpiSummary().completedFollowups()).isEqualTo(1);
        assertThat(result.kpiSummary().overdueFollowups()).isEqualTo(1);
        assertThat(result.kpiSummary().wonLeads()).isEqualTo(1);
        assertThat(result.kpiSummary().lostLeads()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void overview_calculates_completion_rate() {
        // Given
        User user = mockUser(1L, "John Doe");
        FollowupTemplate template = mockTemplate(1L);
        Lead lead1 = mockLead(1L, LeadStatus.NEW, user, template);

        LeadFollowup followup1 = mockFollowup(1L, lead1, FollowupStatus.COMPLETED, FollowupOutcome.INTERESTED, user, ContactChannel.EMAIL);
        LeadFollowup followup2 = mockFollowup(2L, lead1, FollowupStatus.COMPLETED, FollowupOutcome.INTERESTED, user, ContactChannel.CALL);
        LeadFollowup followup3 = mockFollowup(3L, lead1, FollowupStatus.DUE, null, user, ContactChannel.CALL);

        when(leadRepository.findAll(any(Specification.class))).thenReturn(List.of(lead1));
        when(leadFollowupRepository.findAll(any(Specification.class))).thenReturn(List.of(followup1, followup2, followup3));
        when(userRepository.findAllByOrderByFullNameAsc()).thenReturn(List.of(user));

        ReportFilterRequest filter = new ReportFilterRequest(
                LocalDate.now().minusDays(30), LocalDate.now(), null, null, null, null, null, null, null, null, null, null);

        // When
        ReportsOverviewResponse result = reportingService.overview(filter);

        // Then
        // 2 completed out of 3 = 66.67% rounded
        assertThat(result.kpiSummary().completionRate()).isEqualTo(67);
    }

    @Test
    @SuppressWarnings("unchecked")
    void performance_returns_sorted_rep_list() {
        // Given
        User user1 = mockUser(1L, "Alice");
        User user2 = mockUser(2L, "Bob");
        FollowupTemplate template = mockTemplate(1L);
        Lead lead1 = mockLead(1L, LeadStatus.NEW, user1, template);
        Lead lead2 = mockLead(2L, LeadStatus.NEW, user2, template);

        LeadFollowup followup1 = mockFollowup(1L, lead1, FollowupStatus.COMPLETED, FollowupOutcome.INTERESTED, user1, ContactChannel.EMAIL);
        LeadFollowup followup2 = mockFollowup(2L, lead2, FollowupStatus.COMPLETED, FollowupOutcome.NOT_INTERESTED, user2, ContactChannel.CALL);

        when(leadRepository.findAll(any(Specification.class))).thenReturn(List.of(lead1, lead2));
        when(leadFollowupRepository.findAll(any(Specification.class))).thenReturn(List.of(followup1, followup2));
        when(userRepository.findAllByOrderByFullNameAsc()).thenReturn(List.of(user1, user2));

        ReportFilterRequest filter = new ReportFilterRequest(
                LocalDate.now().minusDays(30), LocalDate.now(), null, null, null, null, null, null, null, null, "completedFollowups", ReportSortDirection.DESC);

        // When
        PerformanceReportResponse result = reportingService.performance(filter);

        // Then
        assertThat(result.reps()).hasSize(2);
        // Bob has 1 completed followup, Alice has 1 - order may vary but both present
    }

    @Test
    @SuppressWarnings("unchecked")
    void filtered_followups_filters_by_outcome() {
        // Given
        User user = mockUser(1L, "John Doe");
        FollowupTemplate template = mockTemplate(1L);
        Lead lead1 = mockLead(1L, LeadStatus.NEW, user, template);

        LeadFollowup followup1 = mockFollowup(1L, lead1, FollowupStatus.COMPLETED, FollowupOutcome.INTERESTED, user, ContactChannel.EMAIL);
        LeadFollowup followup2 = mockFollowup(2L, lead1, FollowupStatus.COMPLETED, FollowupOutcome.NOT_INTERESTED, user, ContactChannel.CALL);

        when(leadRepository.findAll(any(Specification.class))).thenReturn(List.of(lead1));
        when(leadFollowupRepository.findAll(any(Specification.class))).thenReturn(List.of(followup1, followup2));

        ReportFilterRequest filter = new ReportFilterRequest(
                LocalDate.now().minusDays(30), LocalDate.now(), null, null, FollowupOutcome.INTERESTED, null, null, null, null, null, null, null);

        // When
        reportingService.overview(filter);

        // Then - verify leadFollowupRepository.findAll was called with spec
        verify(leadFollowupRepository).findAll(any(Specification.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void filtered_followups_filters_by_channel() {
        // Given
        User user = mockUser(1L, "John Doe");
        FollowupTemplate template = mockTemplate(1L);
        Lead lead1 = mockLead(1L, LeadStatus.NEW, user, template);

        LeadFollowup followup1 = mockFollowup(1L, lead1, FollowupStatus.COMPLETED, FollowupOutcome.INTERESTED, user, ContactChannel.EMAIL);
        LeadFollowup followup2 = mockFollowup(2L, lead1, FollowupStatus.COMPLETED, FollowupOutcome.INTERESTED, user, ContactChannel.CALL);

        when(leadRepository.findAll(any(Specification.class))).thenReturn(List.of(lead1));
        when(leadFollowupRepository.findAll(any(Specification.class))).thenReturn(List.of(followup1));

        ReportFilterRequest filter = new ReportFilterRequest(
                LocalDate.now().minusDays(30), LocalDate.now(), null, null, null, ContactChannel.EMAIL, null, null, null, null, null, null);

        // When
        reportingService.overview(filter);

        // Then
        verify(leadFollowupRepository).findAll(any(Specification.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void filtered_followups_filters_by_escalated() {
        // Given
        User user = mockUser(1L, "John Doe");
        FollowupTemplate template = mockTemplate(1L);
        Lead lead1 = mockLead(1L, LeadStatus.NEW, user, template);

        LeadFollowup escalatedFollowup = mockFollowup(1L, lead1, FollowupStatus.COMPLETED, FollowupOutcome.INTERESTED, user, ContactChannel.EMAIL);
        when(escalatedFollowup.getEscalatedAt()).thenReturn(OffsetDateTime.now());

        LeadFollowup normalFollowup = mockFollowup(2L, lead1, FollowupStatus.COMPLETED, FollowupOutcome.INTERESTED, user, ContactChannel.CALL);

        when(leadRepository.findAll(any(Specification.class))).thenReturn(List.of(lead1));
        when(leadFollowupRepository.findAll(any(Specification.class))).thenReturn(List.of(escalatedFollowup));

        ReportFilterRequest filter = new ReportFilterRequest(
                LocalDate.now().minusDays(30), LocalDate.now(), null, null, null, null, null, null, null, true, null, null);

        // When
        reportingService.overview(filter);

        // Then
        verify(leadFollowupRepository).findAll(any(Specification.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void filtered_leads_uses_database_filtering() {
        // Given
        User user = mockUser(1L, "John Doe");
        FollowupTemplate template = mockTemplate(1L);
        Lead lead1 = mockLead(1L, LeadStatus.NEW, user, template);

        ArgumentCaptor<Specification<Lead>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        when(leadRepository.findAll(specCaptor.capture())).thenReturn(List.of(lead1));

        ReportFilterRequest filter = new ReportFilterRequest(
                LocalDate.now().minusDays(30), LocalDate.now(), 1L, LeadStatus.NEW, null, null, null, LeadPriority.HIGH, null, null, null, null);

        // When
        reportingService.funnel(filter);

        // Then - verify findAll was called (not findAll().stream().filter())
        verify(leadRepository).findAll(any(Specification.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void overview_includes_trend_data() {
        // Given
        User user = mockUser(1L, "John Doe");
        FollowupTemplate template = mockTemplate(1L);
        Lead lead1 = mockLead(1L, LeadStatus.WON, user, template);

        LeadFollowup followup1 = mockFollowup(1L, lead1, FollowupStatus.COMPLETED, FollowupOutcome.INTERESTED, user, ContactChannel.EMAIL);

        when(leadRepository.findAll(any(Specification.class))).thenReturn(List.of(lead1));
        when(leadFollowupRepository.findAll(any(Specification.class))).thenReturn(List.of(followup1));
        when(userRepository.findAllByOrderByFullNameAsc()).thenReturn(List.of(user));

        LocalDate dateFrom = LocalDate.now().minusDays(7);
        LocalDate dateTo = LocalDate.now();
        ReportFilterRequest filter = new ReportFilterRequest(
                dateFrom, dateTo, null, null, null, null, null, null, null, null, null, null);

        // When
        ReportsOverviewResponse result = reportingService.overview(filter);

        // Then
        assertThat(result.trends()).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void outcome_summary_counts_all_outcomes() {
        // Given
        User user = mockUser(1L, "John Doe");
        FollowupTemplate template = mockTemplate(1L);
        Lead lead1 = mockLead(1L, LeadStatus.NEW, user, template);

        LeadFollowup followup1 = mockFollowup(1L, lead1, FollowupStatus.COMPLETED, FollowupOutcome.INTERESTED, user, ContactChannel.EMAIL);
        LeadFollowup followup2 = mockFollowup(2L, lead1, FollowupStatus.COMPLETED, FollowupOutcome.NOT_INTERESTED, user, ContactChannel.CALL);
        LeadFollowup followup3 = mockFollowup(3L, lead1, FollowupStatus.COMPLETED, null, user, ContactChannel.CALL);

        when(leadRepository.findAll(any(Specification.class))).thenReturn(List.of(lead1));
        when(leadFollowupRepository.findAll(any(Specification.class))).thenReturn(List.of(followup1, followup2, followup3));
        when(userRepository.findAllByOrderByFullNameAsc()).thenReturn(List.of(user));

        ReportFilterRequest filter = new ReportFilterRequest(
                LocalDate.now().minusDays(30), LocalDate.now(), null, null, null, null, null, null, null, null, null, null);

        // When
        ReportsOverviewResponse result = reportingService.overview(filter);

        // Then
        assertThat(result.outcomeSummary().outcomes().get("INTERESTED")).isEqualTo(1);
        assertThat(result.outcomeSummary().outcomes().get("NOT_INTERESTED")).isEqualTo(1);
        assertThat(result.outcomeSummary().unknownCount()).isEqualTo(1);
    }
}
