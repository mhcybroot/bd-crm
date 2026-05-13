package com.bdcrm.reporting;

import com.bdcrm.followup.FollowupOutcome;
import com.bdcrm.followup.FollowupSpecifications;
import com.bdcrm.followup.FollowupStatus;
import com.bdcrm.followup.LeadFollowup;
import com.bdcrm.followup.LeadFollowupRepository;
import com.bdcrm.lead.Lead;
import com.bdcrm.lead.LeadPriority;
import com.bdcrm.lead.LeadRepository;
import com.bdcrm.lead.LeadSpecifications;
import com.bdcrm.lead.LeadStatus;
import com.bdcrm.template.ContactChannel;
import com.bdcrm.user.User;
import com.bdcrm.user.UserRepository;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final LeadRepository leadRepository;
    private final LeadFollowupRepository leadFollowupRepository;
    private final UserRepository userRepository;

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
        List<PerformanceReportResponse.RepPerformance> reps = userRepository.findAllByOrderByFullNameAsc().stream()
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
        List<PerformanceReportResponse.RepPerformance> reps = userRepository.findAllByOrderByFullNameAsc().stream()
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
        Specification<Lead> spec = buildLeadSpecification(filter);
        return leadRepository.findAll(spec);
    }

    private Specification<Lead> buildLeadSpecification(ReportFilterRequest filter) {
        Specification<Lead> spec = Specification.where(null);

        // Date range filter
        spec = spec.and(LeadSpecifications.createdBetween(filter.effectiveDateFrom(), filter.effectiveDateTo()));

        // Rep user filter
        if (filter.repUserId() != null) {
            spec = spec.and(LeadSpecifications.assignedTo(filter.repUserId()));
        }

        // Status filter
        if (filter.leadStatus() != null) {
            spec = spec.and(LeadSpecifications.hasStatus(filter.leadStatus()));
        }

        // Source filter
        if (filter.source() != null && !filter.source().isBlank()) {
            spec = spec.and(LeadSpecifications.source(filter.source()));
        }

        // Template filter
        if (filter.templateId() != null) {
            spec = spec.and(LeadSpecifications.template(filter.templateId()));
        }

        // Priority filter
        if (filter.priority() != null) {
            spec = spec.and(LeadSpecifications.priority(filter.priority()));
        }

        return spec;
    }

    private List<LeadFollowup> filteredFollowups(ReportFilterRequest filter) {
        Specification<LeadFollowup> spec = buildFollowupSpecification(filter);
        return leadFollowupRepository.findAll(spec);
    }

    private Specification<LeadFollowup> buildFollowupSpecification(ReportFilterRequest filter) {
        Specification<LeadFollowup> spec = Specification.where(null);

        // Filter by lead criteria (date range, rep, status, source, template, priority)
        // These are applied via the lead association
        spec = spec.and(buildLeadAssociationSpecification(filter));

        // Followup outcome filter
        if (filter.followupOutcome() != null) {
            spec = spec.and(FollowupSpecifications.hasOutcome(filter.followupOutcome().name()));
        }

        // Channel filter
        if (filter.channel() != null) {
            spec = spec.and(followupChannelEquals(filter.channel()));
        }

        // Escalated filter
        if (filter.escalated() != null) {
            spec = spec.and(escalated(filter.escalated()));
        }

        return spec;
    }

    /**
     * Builds a specification that filters followups by their lead's attributes.
     * This applies the same lead-level filters (date range, rep, status, source, template, priority)
     * to the associated leads.
     */
    private Specification<LeadFollowup> buildLeadAssociationSpecification(ReportFilterRequest filter) {
        return (root, query, builder) -> {
            var lead = root.get("lead");

            // Date range filter on lead's createdAt
            var createdAt = lead.get("createdAt");
            var dateFrom = filter.effectiveDateFrom();
            var dateTo = filter.effectiveDateTo();

            query.distinct(true);

            var predicates = builder conjunction();

            // Date range
            if (dateFrom != null) {
                var startOfDay = dateFrom.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
                predicates = builder.and(predicates, builder.greaterThanOrEqualTo(createdAt, startOfDay));
            }
            if (dateTo != null) {
                var endOfDay = dateTo.plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
                predicates = builder.and(predicates, builder.lessThan(createdAt, endOfDay));
            }

            // Rep user filter
            if (filter.repUserId() != null) {
                predicates = builder.and(predicates, builder.equal(lead.get("assignedUser").get("id"), filter.repUserId()));
            }

            // Lead status filter
            if (filter.leadStatus() != null) {
                predicates = builder.and(predicates, builder.equal(lead.get("status"), filter.leadStatus()));
            }

            // Source filter
            if (filter.source() != null && !filter.source().isBlank()) {
                predicates = builder.and(predicates,
                        builder.equal(builder.lower(lead.get("source")), filter.source().trim().toLowerCase()));
            }

            // Template filter
            if (filter.templateId() != null) {
                predicates = builder.and(predicates, builder.equal(lead.get("template").get("id"), filter.templateId()));
            }

            // Priority filter
            if (filter.priority() != null) {
                predicates = builder.and(predicates, builder.equal(lead.get("priority"), filter.priority()));
            }

            return predicates;
        };
    }

    private Specification<LeadFollowup> followupChannelEquals(ContactChannel channel) {
        return (root, query, builder) -> channel == null ? null : builder.equal(root.get("channel"), channel);
    }

    private Specification<LeadFollowup> escalated(boolean escalated) {
        return (root, query, builder) -> escalated
                ? builder.isNotNull(root.get("escalatedAt"))
                : builder.isNull(root.get("escalatedAt"));
    }

    private OutcomeSummaryResponse outcomeSummary(List<LeadFollowup> followups) {
        Map<String, Long> outcomes = new LinkedHashMap<>();
        for (var outcome : FollowupOutcome.values()) {
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
