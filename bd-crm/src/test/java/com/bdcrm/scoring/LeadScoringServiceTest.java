package com.bdcrm.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdcrm.common.ApiException;
import com.bdcrm.followup.FollowupOutcome;
import com.bdcrm.followup.FollowupStatus;
import com.bdcrm.followup.LeadFollowup;
import com.bdcrm.followup.LeadFollowupRepository;
import com.bdcrm.lead.Lead;
import com.bdcrm.lead.LeadActivity;
import com.bdcrm.lead.LeadActivityRepository;
import com.bdcrm.lead.LeadActivityType;
import com.bdcrm.lead.LeadRepository;
import com.bdcrm.qualification.LeadQualification;
import com.bdcrm.qualification.LeadQualificationRepository;
import com.bdcrm.template.FollowupTemplate;
import com.bdcrm.user.User;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadScoringServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadFollowupRepository followupRepository;

    @Mock
    private LeadActivityRepository activityRepository;

    @Mock
    private LeadQualificationRepository qualificationRepository;

    private LeadScoringService scoringService;

    private User testUser;
    private Lead testLead;
    private FollowupTemplate testTemplate;

    @BeforeEach
    void setUp() {
        scoringService = new LeadScoringService(
                leadRepository, followupRepository, activityRepository, qualificationRepository);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("rep");
        testUser.setEmail("rep@test.com");
        testUser.setFullName("Test Rep");
        testUser.setActive(true);

        testTemplate = new FollowupTemplate();
        testTemplate.setId(1L);
        testTemplate.setName("Test Template");

        testLead = new Lead();
        testLead.setId(1L);
        testLead.setCompanyName("Test Company");
        testLead.setContactName("John Doe");
        testLead.setEmail("contact@test.com");
        testLead.setAssignedUser(testUser);
        testLead.setTemplate(testTemplate);
        testLead.setCreatedAt(OffsetDateTime.now().minusDays(30));
    }

    @Test
    void calculateEngagementScore_returnsZero_whenNoFollowupsNoActivities() {
        when(followupRepository.findByLeadIdOrderByStepNumberAsc(1L)).thenReturn(List.of());
        when(activityRepository.findByLeadIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        int score = scoringService.calculateEngagementScore(testLead);

        assertThat(score).isEqualTo(0);
    }

    @Test
    void calculateEngagementScore_returnsPerfectScore_whenAllFollowupsCompletedOnTime() {
        // 4 completed followups, all on time (due yesterday, completed yesterday)
        LeadFollowup f1 = createFollowup(1, LocalDate.now().minusDays(5), LocalDate.now().minusDays(5), FollowupStatus.COMPLETED);
        LeadFollowup f2 = createFollowup(2, LocalDate.now().minusDays(3), LocalDate.now().minusDays(3), FollowupStatus.COMPLETED);
        LeadFollowup f3 = createFollowup(3, LocalDate.now().minusDays(1), LocalDate.now().minusDays(1), FollowupStatus.COMPLETED);
        LeadFollowup f4 = createFollowup(4, LocalDate.now(), LocalDate.now(), FollowupStatus.COMPLETED);

        when(followupRepository.findByLeadIdOrderByStepNumberAsc(1L)).thenReturn(List.of(f1, f2, f3, f4));
        when(activityRepository.findByLeadIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        int score = scoringService.calculateEngagementScore(testLead);

        // Follow-up completion: 4/4 = 100% * 40 = 40
        // Communication frequency: 0 * 30 = 0
        // Stage velocity: 0 * 20 = 0
        // Response timeliness: 100% * 10 = 10
        // Total: 50
        assertThat(score).isEqualTo(50);
    }

    @Test
    void calculateEngagementScore_returnsZero_whenNoFollowupsCompleted() {
        // 4 followups, none completed
        LeadFollowup f1 = createFollowup(1, LocalDate.now().minusDays(5), null, FollowupStatus.DUE);
        LeadFollowup f2 = createFollowup(2, LocalDate.now().minusDays(3), null, FollowupStatus.DUE);
        LeadFollowup f3 = createFollowup(3, LocalDate.now().minusDays(1), null, FollowupStatus.DUE);
        LeadFollowup f4 = createFollowup(4, LocalDate.now(), null, FollowupStatus.DUE);

        when(followupRepository.findByLeadIdOrderByStepNumberAsc(1L)).thenReturn(List.of(f1, f2, f3, f4));
        when(activityRepository.findByLeadIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        int score = scoringService.calculateEngagementScore(testLead);

        // Follow-up completion: 0/4 = 0% * 40 = 0
        // Communication frequency: 0 * 30 = 0
        // Stage velocity: 0 * 20 = 0
        // Response timeliness: 0% * 10 = 0
        // Total: 0
        assertThat(score).isEqualTo(0);
    }

    @Test
    void calculateEngagementScore_calculatesPartialFollowupCompletion() {
        // 2 completed, 2 not completed
        LeadFollowup f1 = createFollowup(1, LocalDate.now().minusDays(5), LocalDate.now().minusDays(5), FollowupStatus.COMPLETED);
        LeadFollowup f2 = createFollowup(2, LocalDate.now().minusDays(3), LocalDate.now().minusDays(3), FollowupStatus.COMPLETED);
        LeadFollowup f3 = createFollowup(3, LocalDate.now().minusDays(1), null, FollowupStatus.DUE);
        LeadFollowup f4 = createFollowup(4, LocalDate.now(), null, FollowupStatus.DUE);

        when(followupRepository.findByLeadIdOrderByStepNumberAsc(1L)).thenReturn(List.of(f1, f2, f3, f4));
        when(activityRepository.findByLeadIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        int score = scoringService.calculateEngagementScore(testLead);

        // Follow-up completion: 2/4 = 50% * 40 = 20
        // Communication frequency: 0 * 30 = 0
        // Stage velocity: 0 * 20 = 0
        // Response timeliness: 2/2 = 100% (completed ones all on time) * 10 = 10
        // Total: 30
        assertThat(score).isEqualTo(30);
    }

    @Test
    void calculateEngagementScore_accountsForCommunicationFrequency() {
        // 7 activities in last 7 days
        OffsetDateTime now = OffsetDateTime.now();
        List<LeadActivity> activities = List.of(
                createActivity(now.minusDays(1)),
                createActivity(now.minusDays(2)),
                createActivity(now.minusDays(3)),
                createActivity(now.minusDays(4)),
                createActivity(now.minusDays(5)),
                createActivity(now.minusDays(6)),
                createActivity(now.minusDays(7))
        );

        when(followupRepository.findByLeadIdOrderByStepNumberAsc(1L)).thenReturn(List.of());
        when(activityRepository.findByLeadIdOrderByCreatedAtDesc(1L)).thenReturn(activities);

        int score = scoringService.calculateEngagementScore(testLead);

        // Follow-up completion: 0 * 40 = 0
        // Communication frequency: 7 touchpoints * some weight, max 30 points
        // Stage velocity: 0 * 20 = 0
        // Response timeliness: 0 * 10 = 0
        // Communication frequency score should be capped at 30
        assertThat(score).isEqualTo(30); // Max from communication frequency
    }

    @Test
    void calculateEngagementScore_returnsMax100() {
        // Perfect scenario: all followups completed on time + max activities + fast stage progression
        testLead.setCreatedAt(OffsetDateTime.now().minusDays(100)); // 100 days old

        LeadFollowup f1 = createFollowup(1, LocalDate.now().minusDays(10), LocalDate.now().minusDays(10), FollowupStatus.COMPLETED);
        LeadFollowup f2 = createFollowup(2, LocalDate.now().minusDays(8), LocalDate.now().minusDays(8), FollowupStatus.COMPLETED);
        LeadFollowup f3 = createFollowup(3, LocalDate.now().minusDays(6), LocalDate.now().minusDays(6), FollowupStatus.COMPLETED);
        LeadFollowup f4 = createFollowup(4, LocalDate.now().minusDays(4), LocalDate.now().minusDays(4), FollowupStatus.COMPLETED);
        LeadFollowup f5 = createFollowup(5, LocalDate.now().minusDays(2), LocalDate.now().minusDays(2), FollowupStatus.COMPLETED);

        OffsetDateTime now = OffsetDateTime.now();
        List<LeadActivity> activities = List.of(
                createActivity(now.minusDays(1)),
                createActivity(now.minusDays(2)),
                createActivity(now.minusDays(3)),
                createActivity(now.minusDays(4)),
                createActivity(now.minusDays(5)),
                createActivity(now.minusDays(6)),
                createActivity(now.minusDays(7))
        );

        when(followupRepository.findByLeadIdOrderByStepNumberAsc(1L)).thenReturn(List.of(f1, f2, f3, f4, f5));
        when(activityRepository.findByLeadIdOrderByCreatedAtDesc(1L)).thenReturn(activities);

        int score = scoringService.calculateEngagementScore(testLead);

        // Should be capped at 100
        assertThat(score).isLessThanOrEqualTo(100);
    }

    @Test
    void calculateEngagementScore_handlesOverdueFollowups() {
        // 2 completed on time, 2 completed late (overdue)
        LeadFollowup f1 = createFollowup(1, LocalDate.now().minusDays(10), LocalDate.now().minusDays(8), FollowupStatus.COMPLETED); // 2 days late
        LeadFollowup f2 = createFollowup(2, LocalDate.now().minusDays(8), LocalDate.now().minusDays(6), FollowupStatus.COMPLETED); // 2 days late
        LeadFollowup f3 = createFollowup(3, LocalDate.now().minusDays(6), LocalDate.now().minusDays(4), FollowupStatus.COMPLETED); // 2 days late
        LeadFollowup f4 = createFollowup(4, LocalDate.now().minusDays(4), LocalDate.now().minusDays(2), FollowupStatus.COMPLETED); // 2 days late

        when(followupRepository.findByLeadIdOrderByStepNumberAsc(1L)).thenReturn(List.of(f1, f2, f3, f4));
        when(activityRepository.findByLeadIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        int score = scoringService.calculateEngagementScore(testLead);

        // Follow-up completion: 4/4 = 100% * 40 = 40
        // Response timeliness: all completed but late (average 2 days late each)
        // Total should be less than perfect score due to late completions
        assertThat(score).isLessThan(50);
    }

    @Test
    void refreshScore_updatesEngagementScore_whenQualificationExists() {
        LeadQualification qualification = new LeadQualification();
        qualification.setLead(testLead);
        qualification.setFitScore(75);
        qualification.setEngagementScore(50);
        qualification.setTotalScore(125);

        when(leadRepository.findById(1L)).thenReturn(java.util.Optional.of(testLead));
        when(followupRepository.findByLeadIdOrderByStepNumberAsc(1L)).thenReturn(List.of());
        when(activityRepository.findByLeadIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(qualificationRepository.findByLeadId(1L)).thenReturn(java.util.Optional.of(qualification));

        scoringService.refreshScore(1L);

        ArgumentCaptor<LeadQualification> captor = ArgumentCaptor.forClass(LeadQualification.class);
        verify(qualificationRepository, times(1)).save(captor.capture());

        LeadQualification saved = captor.getValue();
        assertThat(saved.getEngagementScore()).isEqualTo(0); // No followups or activities
        assertThat(saved.getTotalScore()).isEqualTo(saved.getFitScore() + saved.getEngagementScore());
    }

    @Test
    void refreshScore_createsNewQualification_whenNoneExists() {
        when(leadRepository.findById(1L)).thenReturn(java.util.Optional.of(testLead));
        when(followupRepository.findByLeadIdOrderByStepNumberAsc(1L)).thenReturn(List.of());
        when(activityRepository.findByLeadIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(qualificationRepository.findByLeadId(1L)).thenReturn(java.util.Optional.empty());

        scoringService.refreshScore(1L);

        verify(qualificationRepository, times(1)).save(any(LeadQualification.class));
    }

    @Test
    void refreshScore_throwsException_whenLeadNotFound() {
        when(leadRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        try {
            scoringService.refreshScore(99L);
            throw new AssertionError("Expected ApiException to be thrown");
        } catch (ApiException e) {
            assertThat(e.getMessage()).contains("Lead not found");
        }
    }

    @Test
    void calculateEngagementScore_returnsZero_whenOnlySkippedOrCancelledFollowups() {
        LeadFollowup f1 = createFollowup(1, LocalDate.now().minusDays(5), LocalDate.now().minusDays(5), FollowupStatus.SKIPPED);
        LeadFollowup f2 = createFollowup(2, LocalDate.now().minusDays(3), LocalDate.now().minusDays(3), FollowupStatus.CANCELLED);

        when(followupRepository.findByLeadIdOrderByStepNumberAsc(1L)).thenReturn(List.of(f1, f2));
        when(activityRepository.findByLeadIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        int score = scoringService.calculateEngagementScore(testLead);

        // Skipped and cancelled followups don't count as completed
        // So completion rate is 0/2 = 0%
        // Total: 0
        assertThat(score).isEqualTo(0);
    }

    private LeadFollowup createFollowup(int stepNumber, LocalDate dueDate, LocalDate completedDate, FollowupStatus status) {
        LeadFollowup followup = new LeadFollowup();
        followup.setId((long) stepNumber);
        followup.setLead(testLead);
        followup.setStepNumber(stepNumber);
        followup.setDueDate(dueDate);
        followup.setAssignedUser(testUser);
        followup.setStatus(status);
        if (completedDate != null) {
            followup.setCompletedAt(completedDate.atStartOfDay().atOffset(ZoneOffset.UTC));
        }
        return followup;
    }

    private LeadActivity createActivity(OffsetDateTime createdAt) {
        LeadActivity activity = new LeadActivity();
        activity.setId(System.nanoTime());
        activity.setLead(testLead);
        activity.setActor(testUser);
        activity.setType(LeadActivityType.NOTE);
        activity.setDescription("Test activity");
        activity.setCreatedAt(createdAt);
        return activity;
    }
}