package com.bdcrm.dashboard;

import com.bdcrm.auth.SecurityUtils;
import com.bdcrm.followup.FollowupOutcome;
import com.bdcrm.followup.FollowupStatus;
import com.bdcrm.followup.LeadFollowup;
import com.bdcrm.followup.LeadFollowupRepository;
import com.bdcrm.lead.Lead;
import com.bdcrm.lead.LeadRepository;
import com.bdcrm.lead.LeadSpecifications;
import com.bdcrm.user.User;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
        boolean managerView = securityUtils.hasAnyRole("PLATFORM_ADMIN", "ORG_ADMIN", "ORG_MANAGER");
        Long organizationId = securityUtils.hasPlatformRole("PLATFORM_ADMIN") ? null : securityUtils.currentOrganizationId();
        List<Lead> leads = leadRepository.findAll(org.springframework.data.jpa.domain.Specification.where(LeadSpecifications.organizationId(organizationId))).stream()
                .filter(lead -> {
                    LocalDate created = lead.getCreatedAt().toLocalDate();
                    return !created.isBefore(filter.effectiveDateFrom()) && !created.isAfter(filter.effectiveDateTo());
                })
                .filter(lead -> filter.repUserId() == null || lead.getAssignedUser().getId().equals(filter.repUserId()))
                .filter(lead -> filter.leadStatus() == null || lead.getStatus() == filter.leadStatus())
                .filter(lead -> managerView || lead.getAssignedUser().getId().equals(currentUser.getId()))
                .toList();
        List<LeadFollowup> filteredFollowups = (organizationId == null
                ? leadFollowupRepository.findAll()
                : leadFollowupRepository.findByOrganizationIdAndStatusInOrderByDueDateAsc(
                        organizationId,
                        List.of(FollowupStatus.DUE, FollowupStatus.OVERDUE, FollowupStatus.COMPLETED, FollowupStatus.SKIPPED, FollowupStatus.CANCELLED)))
                .stream()
                .filter(followup -> leads.stream().anyMatch(lead -> lead.getId().equals(followup.getLead().getId())))
                .filter(followup -> filter.followupOutcome() == null || followup.getOutcome() == filter.followupOutcome())
                .filter(followup -> managerView || followup.getAssignedUser().getId().equals(currentUser.getId()))
                .toList();
        List<LeadFollowup> queue = filteredFollowups.stream()
                .filter(followup -> followup.getStatus() == FollowupStatus.DUE || followup.getStatus() == FollowupStatus.OVERDUE)
                .toList();
        Map<String, Long> leadsByStatus = Arrays.stream(com.bdcrm.lead.LeadStatus.values())
                .collect(Collectors.toMap(Enum::name,
                        status -> leads.stream().filter(lead -> lead.getStatus() == status).count()));
        long dueToday = queue.stream().filter(followup -> followup.getDueDate().isEqual(LocalDate.now())).count();
        long overdue = queue.stream().filter(followup -> followup.getStatus() == FollowupStatus.OVERDUE).count();
        long escalated = queue.stream().filter(followup -> followup.getEscalatedAt() != null).count();
        long completedFollowups = filteredFollowups.stream().filter(followup -> followup.getStatus() == FollowupStatus.COMPLETED).count();
        long completionRate = filteredFollowups.isEmpty() ? 0 : Math.round((completedFollowups * 100.0) / filteredFollowups.size());
        Map<String, Long> topOutcomes = Arrays.stream(FollowupOutcome.values())
                .collect(Collectors.toMap(Enum::name, outcome -> filteredFollowups.stream().filter(f -> f.getOutcome() == outcome).count()));
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

    @Transactional(readOnly = true)
    public List<DueFollowupResponse> workQueue(DashboardFilterRequest filter) {
        User currentUser = securityUtils.currentUserEntity();
        boolean managerView = securityUtils.hasAnyRole("PLATFORM_ADMIN", "ORG_ADMIN", "ORG_MANAGER");
        Long organizationId = securityUtils.hasPlatformRole("PLATFORM_ADMIN") ? null : securityUtils.currentOrganizationId();
        LocalDate today = LocalDate.now();
        return (organizationId == null
                ? leadFollowupRepository.findByStatusInOrderByDueDateAsc(List.of(FollowupStatus.DUE, FollowupStatus.OVERDUE))
                : leadFollowupRepository.findByOrganizationIdAndStatusInOrderByDueDateAsc(
                        organizationId,
                        List.of(FollowupStatus.DUE, FollowupStatus.OVERDUE)))
                .stream()
                .filter(followup -> followup.getStatus() == FollowupStatus.OVERDUE
                        || (followup.getStatus() == FollowupStatus.DUE && !followup.getDueDate().isAfter(today)))
                .filter(followup -> {
                    LocalDate created = followup.getLead().getCreatedAt().toLocalDate();
                    return !created.isBefore(filter.effectiveDateFrom()) && !created.isAfter(filter.effectiveDateTo());
                })
                .filter(followup -> filter.repUserId() == null || followup.getAssignedUser().getId().equals(filter.repUserId()))
                .filter(followup -> filter.leadStatus() == null || followup.getLead().getStatus() == filter.leadStatus())
                .filter(followup -> filter.followupOutcome() == null || followup.getOutcome() == filter.followupOutcome())
                .filter(followup -> managerView || followup.getAssignedUser().getId().equals(currentUser.getId()))
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
}
