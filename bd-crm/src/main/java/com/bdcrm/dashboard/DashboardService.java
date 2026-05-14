package com.bdcrm.dashboard;

import com.bdcrm.auth.SecurityUtils;
import com.bdcrm.followup.FollowupOutcome;
import com.bdcrm.followup.FollowupStatus;
import com.bdcrm.followup.LeadFollowup;
import com.bdcrm.followup.LeadFollowupRepository;
import com.bdcrm.lead.Lead;
import com.bdcrm.lead.LeadSpecifications;
import com.bdcrm.lead.LeadRepository;
import com.bdcrm.user.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final LeadRepository leadRepository;
    private final LeadFollowupRepository leadFollowupRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(DashboardFilterRequest filter) {
        User currentUser = securityUtils.currentUserEntity();
        boolean managerView = securityUtils.hasAnyRole("ADMIN", "MANAGER");

        Specification<Lead> leadSpec = buildLeadSpec(filter, currentUser, managerView);
        List<Lead> leads = leadRepository.findAll(leadSpec);

        Specification<LeadFollowup> followupSpec = buildFollowupSpec(filter, currentUser, managerView);
        List<LeadFollowup> filteredFollowups = leadFollowupRepository.findAll(followupSpec);

        List<LeadFollowup> queue = filteredFollowups.stream()
                .filter(f -> f.getStatus() == FollowupStatus.DUE || f.getStatus() == FollowupStatus.OVERDUE)
                .toList();

        Map<String, Long> leadsByStatus = Arrays.stream(Lead.LeadStatus.values())
                .collect(Collectors.toMap(Enum::name,
                        status -> leads.stream().filter(lead -> lead.getStatus() == status).count()));

        long dueToday = queue.stream()
                .filter(f -> f.getDueDate().isEqual(LocalDate.now()))
                .count();
        long overdue = queue.stream()
                .filter(f -> f.getStatus() == FollowupStatus.OVERDUE)
                .count();
        long escalated = queue.stream()
                .filter(f -> f.getEscalatedAt() != null)
                .count();
        long completedFollowups = filteredFollowups.stream()
                .filter(f -> f.getStatus() == FollowupStatus.COMPLETED)
                .count();
        long completionRate = filteredFollowups.isEmpty() ? 0
                : Math.round((completedFollowups * 100.0) / filteredFollowups.size());

        Map<String, Long> topOutcomes = Arrays.stream(FollowupOutcome.values())
                .collect(Collectors.toMap(Enum::name,
                        outcome -> filteredFollowups.stream()
                                .filter(f -> f.getOutcome() == outcome)
                                .count()));

        return new DashboardSummaryResponse(
                leads.size(),
                leads.size(),
                leadsByStatus,
                dueToday,
                overdue,
                escalated,
                completedFollowups,
                completionRate,
                topOutcomes);
    }

    private Specification<Lead> buildLeadSpec(DashboardFilterRequest filter, User currentUser, boolean managerView) {
        Specification<Lead> spec = Specification.where(null);

        LocalDateTime fromDateTime = filter.effectiveDateFrom().atStartOfDay();
        LocalDateTime toDateTime = filter.effectiveDateTo().atTime(LocalTime.MAX);

        spec = spec.and(LeadSpecifications.createdBetween(
                fromDateTime.atZone(ZoneId.systemDefault()).toInstant(),
                toDateTime.atZone(ZoneId.systemDefault()).toInstant()));

        if (!managerView) {
            spec = spec.and(LeadSpecifications.assignedTo(currentUser.getId()));
        } else if (filter.repUserId() != null) {
            spec = spec.and(LeadSpecifications.assignedTo(filter.repUserId()));
        }

        if (filter.leadStatus() != null && !filter.leadStatus().isBlank()) {
            try {
                var status = Lead.LeadStatus.valueOf(filter.leadStatus());
                spec = spec.and(LeadSpecifications.hasStatus(status));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return spec;
    }

    private Specification<LeadFollowup> buildFollowupSpec(DashboardFilterRequest filter, User currentUser, boolean managerView) {
        return (followup, query, cb) -> {
            var lead = followup.get("lead");

            var dateFromPred = cb.greaterThanOrEqualTo(
                    lead.get("createdAt"),
                    filter.effectiveDateFrom().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
            var dateToPred = cb.lessThanOrEqualTo(
                    lead.get("createdAt"),
                    filter.effectiveDateTo().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());

            if (!managerView) {
                var userPred = cb.equal(followup.get("assignedUser").get("id"), currentUser.getId());
                return cb.and(dateFromPred, dateToPred, userPred);
            } else if (filter.repUserId() != null) {
                var repPred = cb.equal(followup.get("assignedUser").get("id"), filter.repUserId());
                return cb.and(dateFromPred, dateToPred, repPred);
            }

            return cb.and(dateFromPred, dateToPred);
        };
    }

    @Transactional(readOnly = true)
    public List<DueFollowupResponse> workQueue(DashboardFilterRequest filter) {
        User currentUser = securityUtils.currentUserEntity();
        boolean managerView = securityUtils.hasAnyRole("ADMIN", "MANAGER");
        LocalDate today = LocalDate.now();

        Specification<LeadFollowup> spec = buildWorkQueueSpec(filter, currentUser, managerView, today);

        return leadFollowupRepository.findAll(spec).stream()
                .map(followup -> new DueFollowupResponse(
                        followup.getId(),
                        followup.getLead().getId(),
                        followup.getLead().getCompanyName(),
                        followup.getLead().getContactName(),
                        followup.getStepNumber(),
                        followup.getDueDate(),
                        followup.getStatus(),
                        followup.getChannel(),
                        followup.getAssignedUser().getId(),
                        followup.getAssignedUser().getFullName(),
                        followup.getEscalatedAt() != null))
                .toList();
    }

    private Specification<LeadFollowup> buildWorkQueueSpec(
            DashboardFilterRequest filter,
            User currentUser,
            boolean managerView,
            LocalDate today) {

        return (followup, query, cb) -> {
            var lead = followup.get("lead");

            var dueOrOverdue = cb.or(
                    cb.equal(followup.get("status"), FollowupStatus.OVERDUE),
                    cb.and(
                            cb.equal(followup.get("status"), FollowupStatus.DUE),
                            cb.lessThanOrEqualTo(followup.get("dueDate"), today)));

            var dateFromPred = cb.greaterThanOrEqualTo(
                    lead.get("createdAt"),
                    filter.effectiveDateFrom().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
            var dateToPred = cb.lessThanOrEqualTo(
                    lead.get("createdAt"),
                    filter.effectiveDateTo().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());

            if (!managerView) {
                var userPred = cb.equal(followup.get("assignedUser").get("id"), currentUser.getId());
                return cb.and(dueOrOverdue, dateFromPred, dateToPred, userPred);
            } else if (filter.repUserId() != null) {
                var repPred = cb.equal(followup.get("assignedUser").get("id"), filter.repUserId());
                return cb.and(dueOrOverdue, dateFromPred, dateToPred, repPred);
            }

            return cb.and(dueOrOverdue, dateFromPred, dateToPred);
        };
    }
}