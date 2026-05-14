package com.bdcrm.reporting;

import com.bdcrm.auth.SecurityUtils;
import com.bdcrm.followup.FollowupStatus;
import com.bdcrm.followup.LeadFollowup;
import com.bdcrm.followup.LeadFollowupRepository;
import com.bdcrm.lead.Lead;
import com.bdcrm.lead.LeadRepository;
import com.bdcrm.lead.LeadSpecifications;
import com.bdcrm.lead.LeadStatus;
import com.bdcrm.user.User;
import com.bdcrm.user.UserRepository;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final LeadRepository leadRepository;
    private final LeadFollowupRepository leadFollowupRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public Map<String, Long> funnel(ReportFilterRequest filter) {
        List<Lead> leads = filteredLeads(filter);
        return Arrays.stream(LeadStatus.values())
                .collect(Collectors.toMap(Enum::name, status -> leads.stream().filter(lead -> lead.getStatus() == status).count()));
    }

    @Transactional(readOnly = true)
    public PerformanceReportResponse performance(ReportFilterRequest filter) {
        List<Lead> leads = filteredLeads(filter);
        List<LeadFollowup> followups = filteredFollowups(filter);
        long completed = followups.stream().filter(followup -> followup.getStatus() == FollowupStatus.COMPLETED).count();
        long completionRate = followups.isEmpty() ? 0 : Math.round((completed * 100.0) / followups.size());
        List<PerformanceReportResponse.RepPerformance> reps = visibleUsers().stream()
                .map(user -> toRepPerformance(user, leads, followups))
                .sorted(repComparator(filter.effectiveSortBy(), filter.effectiveSortDirection()))
                .toList();
        return new PerformanceReportResponse(
                funnel(filter),
                completionRate,
                outcomeSummary(followups),
                buildTrends(leads, followups, filter),
                reps);
    }

    @Transactional(readOnly = true)
    public ReportsOverviewResponse overview(ReportFilterRequest filter) {
        List<Lead> leads = filteredLeads(filter);
        List<LeadFollowup> followups = filteredFollowups(filter);
        long completed = followups.stream().filter(followup -> followup.getStatus() == FollowupStatus.COMPLETED).count();
        long completionRate = followups.isEmpty() ? 0 : Math.round((completed * 100.0) / followups.size());
        long overdue = followups.stream().filter(f -> f.getStatus() == FollowupStatus.OVERDUE).count();
        long wonLeads = leads.stream().filter(lead -> lead.getStatus() == LeadStatus.WON).count();
        long lostLeads = leads.stream().filter(lead -> lead.getStatus() == LeadStatus.LOST).count();
        List<PerformanceReportResponse.RepPerformance> reps = visibleUsers().stream()
                .map(user -> toRepPerformance(user, leads, followups))
                .sorted(repComparator(filter.effectiveSortBy(), filter.effectiveSortDirection()))
                .toList();
        return new ReportsOverviewResponse(
                new ReportKpiSummaryResponse(
                        leads.size(),
                        followups.size(),
                        completed,
                        overdue,
                        completionRate,
                        wonLeads,
                        lostLeads),
                funnel(filter),
                outcomeSummary(followups),
                buildTrends(leads, followups, filter),
                reps);
    }

    private PerformanceReportResponse.RepPerformance toRepPerformance(User user, List<Lead> leads, List<LeadFollowup> followups) {
        long assignedLeads = leads.stream().filter(lead -> lead.getAssignedUser().getId().equals(user.getId())).count();
        long pendingFollowups = followups.stream()
                .filter(followup -> followup.getAssignedUser().getId().equals(user.getId()))
                .filter(followup -> followup.getStatus() == FollowupStatus.DUE || followup.getStatus() == FollowupStatus.OVERDUE)
                .count();
        long completedFollowups = followups.stream()
                .filter(followup -> followup.getAssignedUser().getId().equals(user.getId()))
                .filter(followup -> followup.getStatus() == FollowupStatus.COMPLETED)
                .count();
        return new PerformanceReportResponse.RepPerformance(
                user.getId(),
                user.getFullName(),
                assignedLeads,
                pendingFollowups,
                completedFollowups);
    }

    private List<Lead> filteredLeads(ReportFilterRequest filter) {
        Long organizationId = securityUtils.hasPlatformRole("PLATFORM_ADMIN") ? null : securityUtils.currentOrganizationId();
        return leadRepository.findAll(org.springframework.data.jpa.domain.Specification.where(LeadSpecifications.organizationId(organizationId))).stream()
                .filter(lead -> {
                    var created = lead.getCreatedAt().toLocalDate();
                    return !created.isBefore(filter.effectiveDateFrom()) && !created.isAfter(filter.effectiveDateTo());
                })
                .filter(lead -> filter.repUserId() == null || lead.getAssignedUser().getId().equals(filter.repUserId()))
                .filter(lead -> filter.leadStatus() == null || lead.getStatus() == filter.leadStatus())
                .filter(lead -> filter.source() == null || filter.source().isBlank() || (lead.getSource() != null && lead.getSource().equalsIgnoreCase(filter.source().trim())))
                .filter(lead -> filter.templateId() == null || lead.getTemplate().getId().equals(filter.templateId()))
                .filter(lead -> filter.priority() == null || lead.getPriority() == filter.priority())
                .toList();
    }

    private List<LeadFollowup> filteredFollowups(ReportFilterRequest filter) {
        List<Lead> filteredLeads = filteredLeads(filter);
        Map<Long, Lead> leadMap = filteredLeads.stream().collect(Collectors.toMap(Lead::getId, lead -> lead));
        Long organizationId = securityUtils.hasPlatformRole("PLATFORM_ADMIN") ? null : securityUtils.currentOrganizationId();
        return visibleFollowups(organizationId).stream()
                .filter(followup -> leadMap.containsKey(followup.getLead().getId()))
                .filter(followup -> filter.followupOutcome() == null || followup.getOutcome() == filter.followupOutcome())
                .filter(followup -> filter.channel() == null || followup.getChannel() == filter.channel())
                .filter(followup -> filter.escalated() == null || (filter.escalated() ? followup.getEscalatedAt() != null : followup.getEscalatedAt() == null))
                .toList();
    }

    private List<User> visibleUsers() {
        return securityUtils.hasPlatformRole("PLATFORM_ADMIN")
                ? userRepository.findAllByOrderByFullNameAsc()
                : userRepository.findAllByOrganizationIdOrderByFullNameAsc(securityUtils.currentOrganizationId());
    }

    private List<LeadFollowup> visibleFollowups(Long organizationId) {
        return organizationId == null
                ? leadFollowupRepository.findAll()
                : leadFollowupRepository.findByOrganizationIdAndStatusInOrderByDueDateAsc(
                        organizationId,
                        List.of(FollowupStatus.DUE, FollowupStatus.OVERDUE, FollowupStatus.COMPLETED, FollowupStatus.SKIPPED, FollowupStatus.CANCELLED));
    }

    private OutcomeSummaryResponse outcomeSummary(List<LeadFollowup> followups) {
        Map<String, Long> outcomes = new LinkedHashMap<>();
        for (var outcome : com.bdcrm.followup.FollowupOutcome.values()) {
            outcomes.put(outcome.name(), followups.stream().filter(followup -> followup.getOutcome() == outcome).count());
        }
        long unknownCount = followups.stream()
                .filter(followup -> followup.getStatus() == FollowupStatus.COMPLETED)
                .filter(followup -> followup.getOutcome() == null)
                .count();
        return new OutcomeSummaryResponse(outcomes, unknownCount);
    }

    private List<TrendPointResponse> buildTrends(List<Lead> leads, List<LeadFollowup> followups, ReportFilterRequest filter) {
        List<java.time.LocalDate> days = filter.effectiveDateFrom().datesUntil(filter.effectiveDateTo().plusDays(1)).toList();
        return days.stream().map(day -> new TrendPointResponse(
                day,
                leads.stream().filter(lead -> lead.getCreatedAt().toLocalDate().isEqual(day)).count(),
                followups.stream().filter(followup -> followup.getCompletedAt() != null && followup.getCompletedAt().toLocalDate().isEqual(day)).count(),
                leads.stream().filter(lead -> lead.getStatus() == LeadStatus.WON && lead.getUpdatedAt().toLocalDate().isEqual(day)).count(),
                leads.stream().filter(lead -> lead.getStatus() == LeadStatus.LOST && lead.getUpdatedAt().toLocalDate().isEqual(day)).count()))
                .toList();
    }

    private Comparator<PerformanceReportResponse.RepPerformance> repComparator(String sortBy, ReportSortDirection direction) {
        Comparator<PerformanceReportResponse.RepPerformance> comparator = switch (sortBy) {
            case "assignedLeads" -> Comparator.comparingLong(PerformanceReportResponse.RepPerformance::assignedLeads);
            case "pendingFollowups" -> Comparator.comparingLong(PerformanceReportResponse.RepPerformance::pendingFollowups);
            case "userName" -> Comparator.comparing(PerformanceReportResponse.RepPerformance::userName);
            default -> Comparator.comparingLong(PerformanceReportResponse.RepPerformance::completedFollowups);
        };
        return direction == ReportSortDirection.ASC ? comparator : comparator.reversed();
    }
}
