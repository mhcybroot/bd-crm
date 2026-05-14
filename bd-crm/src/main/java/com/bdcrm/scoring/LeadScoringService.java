package com.bdcrm.scoring;

import com.bdcrm.common.ApiException;
import com.bdcrm.followup.FollowupStatus;
import com.bdcrm.followup.LeadFollowup;
import com.bdcrm.followup.LeadFollowupRepository;
import com.bdcrm.lead.Lead;
import com.bdcrm.lead.LeadActivity;
import com.bdcrm.lead.LeadActivityRepository;
import com.bdcrm.lead.LeadRepository;
import com.bdcrm.qualification.LeadQualification;
import com.bdcrm.qualification.LeadQualificationRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeadScoringService {

    private final LeadRepository leadRepository;
    private final LeadFollowupRepository followupRepository;
    private final LeadActivityRepository activityRepository;
    private final LeadQualificationRepository qualificationRepository;

    // Weight factors for engagement score components
    private static final double FOLLOWUP_COMPLETION_WEIGHT = 0.40;
    private static final double COMMUNICATION_FREQUENCY_WEIGHT = 0.30;
    private static final double STAGE_VELOCITY_WEIGHT = 0.20;
    private static final double RESPONSE_TIMELINESS_WEIGHT = 0.10;

    // Maximum points per component (out of 100 total)
    private static final int FOLLOWUP_COMPLETION_MAX = 40;
    private static final int COMMUNICATION_FREQUENCY_MAX = 30;
    private static final int STAGE_VELOCITY_MAX = 20;
    private static final int RESPONSE_TIMELINESS_MAX = 10;

    // Communication frequency constants
    private static final int COMM_FREQUENCY_WINDOW_DAYS = 7;
    private static final int COMM_FREQUENCY_TARGET = 7; // target touchpoints per 7-day window

    /**
     * Auto-calculate engagement score based on behavioral signals.
     * Score range: 0-100
     *
     * Factors:
     * - Follow-up completion rate (40% weight): completed / total assigned
     * - Communication frequency (30% weight): touchpoints per 7-day window
     * - Stage velocity (20% weight): stages advanced / days elapsed
     * - Response timeliness (10% weight): completed on time vs overdue
     */
    public int calculateEngagementScore(Lead lead) {
        List<LeadFollowup> followups = followupRepository.findByLeadIdOrderByStepNumberAsc(lead.getId());
        List<LeadActivity> activities = activityRepository.findByLeadIdOrderByCreatedAtDesc(lead.getId());

        // Factor 1: Follow-up completion rate (40% weight)
        int followupScore = calculateFollowupCompletionScore(followups);

        // Factor 2: Communication frequency (30% weight)
        int communicationScore = calculateCommunicationFrequencyScore(activities);

        // Factor 3: Stage velocity (20% weight)
        int stageVelocityScore = calculateStageVelocityScore(lead);

        // Factor 4: Response timeliness (10% weight)
        int responseTimelinessScore = calculateResponseTimelinessScore(followups);

        int totalScore = followupScore + communicationScore + stageVelocityScore + responseTimelinessScore;

        // Cap at 100
        return Math.min(totalScore, 100);
    }

    /**
     * Calculate follow-up completion score.
     * Score = (completed followups / total followups) * FOLLOWUP_COMPLETION_MAX
     * Only COMPLETED status counts toward completion. SKIPPED, CANCELLED, etc. do not.
     */
    private int calculateFollowupCompletionScore(List<LeadFollowup> followups) {
        if (followups.isEmpty()) {
            return 0;
        }

        long totalAssigned = followups.size();
        long completedCount = followups.stream()
                .filter(f -> f.getStatus() == FollowupStatus.COMPLETED)
                .count();

        double completionRate = (double) completedCount / totalAssigned;
        return (int) (completionRate * FOLLOWUP_COMPLETION_MAX);
    }

    /**
     * Calculate communication frequency score.
     * Score = min(touchpoints in last 7 days / target * COMMUNICATION_FREQUENCY_MAX, COMMUNICATION_FREQUENCY_MAX)
     */
    private int calculateCommunicationFrequencyScore(List<LeadActivity> activities) {
        if (activities.isEmpty()) {
            return 0;
        }

        OffsetDateTime sevenDaysAgo = OffsetDateTime.now().minusDays(COMM_FREQUENCY_WINDOW_DAYS);

        long recentTouchpoints = activities.stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(sevenDaysAgo))
                .count();

        double frequencyRatio = Math.min((double) recentTouchpoints / COMM_FREQUENCY_TARGET, 1.0);
        return (int) (frequencyRatio * COMMUNICATION_FREQUENCY_MAX);
    }

    /**
     * Calculate stage velocity score.
     * Score = min(stages advanced / days elapsed * 100, STAGE_VELOCITY_MAX)
     * For new leads with no stage changes, score is 0.
     */
    private int calculateStageVelocityScore(Lead lead) {
        OffsetDateTime createdAt = lead.getCreatedAt();
        if (createdAt == null) {
            return 0;
        }

        long daysSinceCreation = ChronoUnit.DAYS.between(createdAt, OffsetDateTime.now());
        if (daysSinceCreation <= 0) {
            return 0;
        }

        // Count completed followups as proxy for stage advancement
        // In a real system, you'd track actual stage transitions
        // Here we use followup completion as a proxy
        List<LeadFollowup> followups = followupRepository.findByLeadIdOrderByStepNumberAsc(lead.getId());
        long completedFollowups = followups.stream()
                .filter(f -> f.getStatus() == FollowupStatus.COMPLETED)
                .count();

        // Calculate velocity: completed stages per day * multiplier
        double velocity = (double) completedFollowups / daysSinceCreation;

        // Normalize to score (cap at STAGE_VELOCITY_MAX)
        // Using 0.5 as baseline (0.5 stages per day = good velocity)
        double normalizedVelocity = Math.min(velocity / 0.5, 1.0);
        return (int) (normalizedVelocity * STAGE_VELOCITY_MAX);
    }

    /**
     * Calculate response timeliness score.
     * Score = (completed on time / total completed) * RESPONSE_TIMELINESS_MAX
     * A follow-up is "on time" if completedAt <= dueDate
     */
    private int calculateResponseTimelinessScore(List<LeadFollowup> followups) {
        List<LeadFollowup> completedFollowups = followups.stream()
                .filter(f -> f.getStatus() == FollowupStatus.COMPLETED)
                .filter(f -> f.getCompletedAt() != null)
                .toList();

        if (completedFollowups.isEmpty()) {
            return 0;
        }

        long onTimeCount = completedFollowups.stream()
                .filter(f -> {
                    LocalDate dueDate = f.getDueDate();
                    OffsetDateTime completedAt = f.getCompletedAt();
                    LocalDate completedDate = completedAt.toLocalDate();
                    return !completedDate.isAfter(dueDate); // completed on or before due date
                })
                .count();

        double timelinessRate = (double) onTimeCount / completedFollowups.size();
        return (int) (timelinessRate * RESPONSE_TIMELINESS_MAX);
    }

    @Transactional
    public void refreshScore(Long leadId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Lead not found"));

        int engagement = calculateEngagementScore(lead);

        LeadQualification qualification = qualificationRepository.findByLeadId(leadId)
                .orElseGet(() -> {
                    LeadQualification newQual = new LeadQualification();
                    newQual.setLead(lead);
                    newQual.setFitScore(50); // default fit score
                    return newQual;
                });

        qualification.setEngagementScore(engagement);
        qualification.setTotalScore(qualification.getFitScore() + engagement);
        qualification.setQualificationUpdatedAt(OffsetDateTime.now());

        qualificationRepository.save(qualification);
    }
}