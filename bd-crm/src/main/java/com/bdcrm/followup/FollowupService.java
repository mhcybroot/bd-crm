package com.bdcrm.followup;

import com.bdcrm.auth.SecurityUtils;
import com.bdcrm.common.ApiException;
import com.bdcrm.config.CrmProperties;
import com.bdcrm.lead.Lead;
import com.bdcrm.lead.LeadActivityService;
import com.bdcrm.lead.LeadActivityType;
import com.bdcrm.template.FollowupTemplate;
import com.bdcrm.template.FollowupTemplateStep;
import com.bdcrm.user.User;
import com.bdcrm.user.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowupService {

    private final LeadFollowupRepository leadFollowupRepository;
    private final EscalationEventRepository escalationEventRepository;
    private final UserRepository userRepository;
    private final LeadActivityService leadActivityService;
    private final CrmProperties crmProperties;
    private final SecurityUtils securityUtils;

    @Transactional
    public List<LeadFollowupResponse> syncFromTemplate(Lead lead) {
        FollowupTemplate template = lead.getTemplate();
        if (template.getSteps().size() > 7) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Lead template exceeds 7 follow-up steps");
        }
        Set<Integer> completedSteps = leadFollowupRepository.findByLeadIdOrderByStepNumberAsc(lead.getId()).stream()
                .filter(existing -> existing.getStatus() == FollowupStatus.COMPLETED)
                .map(LeadFollowup::getStepNumber)
                .collect(java.util.stream.Collectors.toSet());

        List<LeadFollowup> openFollowups = leadFollowupRepository.findByLeadIdOrderByStepNumberAsc(lead.getId()).stream()
                .filter(existing -> existing.getStatus() != FollowupStatus.COMPLETED)
                .toList();
        leadFollowupRepository.deleteAll(openFollowups);

        List<LeadFollowup> created = template.getSteps().stream()
                .sorted(Comparator.comparingInt(FollowupTemplateStep::getStepNumber))
                .filter(step -> !completedSteps.contains(step.getStepNumber()))
                .map(step -> toLeadFollowup(lead, step))
                .toList();
        return leadFollowupRepository.saveAll(created).stream().map(LeadFollowupResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LeadFollowupResponse> findForLead(Long leadId) {
        return leadFollowupRepository.findByLeadIdOrderByStepNumberAsc(leadId).stream()
                .map(LeadFollowupResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeadFollowupResponse> listWorkQueue(String statusFilter) {
        String effectiveFilter = statusFilter == null ? "open" : statusFilter.toLowerCase();
        LocalDate today = LocalDate.now();
        User currentUser = securityUtils.currentUserEntity();
        boolean managerView = securityUtils.hasAnyRole("PLATFORM_ADMIN", "ORG_ADMIN", "ORG_MANAGER");
        Long organizationId = securityUtils.hasPlatformRole("PLATFORM_ADMIN") ? null : securityUtils.currentOrganizationId();
        List<LeadFollowup> queue = switch (effectiveFilter) {
            case "due" -> followupsDueByStatus(organizationId, List.of(FollowupStatus.DUE), today);
            case "overdue" -> followupsByStatus(organizationId, List.of(FollowupStatus.OVERDUE));
            case "completed" -> followupsByStatus(organizationId, List.of(FollowupStatus.COMPLETED));
            case "upcoming" -> followupsByStatus(organizationId, List.of(FollowupStatus.DUE)).stream()
                    .filter(followup -> followup.getDueDate().isAfter(today))
                    .toList();
            default -> followupsByStatus(organizationId, List.of(FollowupStatus.DUE, FollowupStatus.OVERDUE)).stream()
                    .filter(followup -> followup.getStatus() == FollowupStatus.OVERDUE
                            || (followup.getStatus() == FollowupStatus.DUE && !followup.getDueDate().isAfter(today)))
                    .toList();
        };
        return queue.stream()
                .filter(followup -> managerView || followup.getAssignedUser().getId().equals(currentUser.getId()))
                .map(LeadFollowupResponse::from)
                .toList();
    }

    @Transactional
    public LeadFollowupResponse complete(Long followupId, FollowupActionRequest request) {
        LeadFollowup followup = requireAccessibleFollowup(followupId);
        ensureMutable(followup);
        if (request.outcome() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Outcome is required when completing a follow-up");
        }
        followup.setStatus(FollowupStatus.COMPLETED);
        followup.setOutcome(request.outcome());
        followup.setNotes(request.notes());
        followup.setCompletedAt(OffsetDateTime.now());
        leadActivityService.log(
                followup.getLead(),
                securityUtils.currentUserEntity(),
                LeadActivityType.FOLLOWUP_COMPLETED,
                "Completed follow-up " + followup.getStepNumber());
        return LeadFollowupResponse.from(followup);
    }

    @Transactional
    public LeadFollowupResponse reschedule(Long followupId, FollowupActionRequest request) {
        LeadFollowup followup = requireAccessibleFollowup(followupId);
        ensureMutable(followup);
        if (request.dueDate() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A new due date is required to reschedule");
        }
        followup.setDueDate(request.dueDate());
        followup.setStatus(FollowupStatus.DUE);
        followup.setNotes(request.notes());
        followup.setEscalatedAt(null);
        leadActivityService.log(
                followup.getLead(),
                securityUtils.currentUserEntity(),
                LeadActivityType.FOLLOWUP_RESCHEDULED,
                "Rescheduled follow-up " + followup.getStepNumber() + " to " + request.dueDate());
        return LeadFollowupResponse.from(followup);
    }

    @Transactional
    public LeadFollowupResponse skip(Long followupId, FollowupActionRequest request) {
        LeadFollowup followup = requireAccessibleFollowup(followupId);
        ensureMutable(followup);
        followup.setStatus(FollowupStatus.SKIPPED);
        followup.setNotes(request.notes());
        leadActivityService.log(
                followup.getLead(),
                securityUtils.currentUserEntity(),
                LeadActivityType.FOLLOWUP_SKIPPED,
                "Skipped follow-up " + followup.getStepNumber());
        return LeadFollowupResponse.from(followup);
    }

    @Transactional
    public LeadFollowupResponse reassign(Long followupId, FollowupActionRequest request) {
        LeadFollowup followup = requireAccessibleFollowup(followupId);
        ensureMutable(followup);
        if (request.assignedUserId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A user is required for reassignment");
        }
        User newOwner = userRepository.findById(request.assignedUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignee not found"));
        if (!securityUtils.hasPlatformRole("PLATFORM_ADMIN")
                && !newOwner.getOrganization().getId().equals(followup.getOrganization().getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Assignee must belong to the same organization");
        }
        followup.setAssignedUser(newOwner);
        followup.setNotes(request.notes());
        leadActivityService.log(
                followup.getLead(),
                securityUtils.currentUserEntity(),
                LeadActivityType.FOLLOWUP_REASSIGNED,
                "Reassigned follow-up " + followup.getStepNumber() + " to " + newOwner.getFullName());
        return LeadFollowupResponse.from(followup);
    }

    @Transactional
    public List<LeadFollowupResponse> bulkAction(BulkFollowupActionRequest request) {
        return request.followupIds().stream().map(id -> {
            return switch ((request.action() == null ? "" : request.action().toLowerCase())) {
                case "complete" -> complete(id, new FollowupActionRequest(null, FollowupOutcome.NO_RESPONSE, null, request.notes()));
                case "skip" -> skip(id, new FollowupActionRequest(null, null, null, request.notes()));
                case "reschedule" -> reschedule(id, new FollowupActionRequest(request.dueDate(), null, null, request.notes()));
                case "reassign" -> reassign(id, new FollowupActionRequest(null, null, request.assignedUserId(), request.notes()));
                default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported bulk follow-up action");
            };
        }).toList();
    }

    @Transactional
    public void closeOpenFollowups(Lead lead, User actor, String reason) {
        List<LeadFollowup> openFollowups = leadFollowupRepository.findByLeadIdOrderByStepNumberAsc(lead.getId()).stream()
                .filter(followup -> followup.getStatus() == FollowupStatus.DUE || followup.getStatus() == FollowupStatus.OVERDUE)
                .toList();
        openFollowups.forEach(followup -> followup.setStatus(FollowupStatus.CANCELLED));
        if (!openFollowups.isEmpty()) {
            leadActivityService.log(lead, actor, LeadActivityType.FOLLOWUPS_CLOSED, reason);
        }
    }

    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void refreshOverdueAndEscalations() {
        LocalDate today = LocalDate.now();
        List<LeadFollowup> openFollowups = leadFollowupRepository.findByStatusInOrderByDueDateAsc(
                List.of(FollowupStatus.DUE, FollowupStatus.OVERDUE));
        for (LeadFollowup followup : openFollowups) {
            if (followup.getDueDate().isBefore(today)) {
                followup.setStatus(FollowupStatus.OVERDUE);
                long daysOverdue = ChronoUnit.DAYS.between(followup.getDueDate(), today);
                if (daysOverdue >= crmProperties.getEscalation().getThresholdDays()
                        && !escalationEventRepository.existsByFollowupId(followup.getId())) {
                    User escalatedTo = followup.getAssignedUser().getManager();
                    EscalationEvent event = new EscalationEvent();
                    if (escalatedTo != null
                            && !escalatedTo.getOrganization().getId().equals(followup.getOrganization().getId())) {
                        escalatedTo = null;
                    }
                    event.setFollowup(followup);
                    event.setLead(followup.getLead());
                    event.setOrganization(followup.getOrganization());
                    event.setEscalatedToUser(escalatedTo);
                    event.setDaysOverdue((int) daysOverdue);
                    event.setReason("Follow-up exceeded overdue threshold");
                    escalationEventRepository.save(event);
                    followup.setEscalatedAt(OffsetDateTime.now());
                    leadActivityService.log(
                            followup.getLead(),
                            escalatedTo != null ? escalatedTo : followup.getAssignedUser(),
                            LeadActivityType.FOLLOWUP_ESCALATED,
                            "Escalated follow-up " + followup.getStepNumber() + " after " + daysOverdue + " overdue day(s)");
                }
            }
        }
    }

    private LeadFollowup toLeadFollowup(Lead lead, FollowupTemplateStep step) {
        if (leadFollowupRepository.existsByLeadIdAndStepNumber(lead.getId(), step.getStepNumber())) {
            throw new ApiException(HttpStatus.CONFLICT, "Follow-up step already exists for lead");
        }
        LeadFollowup followup = new LeadFollowup();
        followup.setLead(lead);
        followup.setStepNumber(step.getStepNumber());
        followup.setDueDate(LocalDate.now().plusDays(step.getDayOffset()));
        followup.setAssignedUser(lead.getAssignedUser());
        followup.setStatus(FollowupStatus.DUE);
        followup.setChannel(step.getChannel());
        followup.setInstructions(step.getInstructions());
        followup.setOrganization(lead.getOrganization());
        return followup;
    }

    private LeadFollowup requireAccessibleFollowup(Long followupId) {
        LeadFollowup followup = securityUtils.hasPlatformRole("PLATFORM_ADMIN")
                ? leadFollowupRepository.findById(followupId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Follow-up not found"))
                : leadFollowupRepository.findByIdAndOrganizationId(followupId, securityUtils.currentOrganizationId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Follow-up not found"));
        User currentUser = securityUtils.currentUserEntity();
        if (securityUtils.hasAnyRole("PLATFORM_ADMIN", "ORG_ADMIN", "ORG_MANAGER")
                || followup.getAssignedUser().getId().equals(currentUser.getId())) {
            return followup;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this follow-up");
    }

    private List<LeadFollowup> followupsByStatus(Long organizationId, List<FollowupStatus> statuses) {
        return organizationId == null
                ? leadFollowupRepository.findByStatusInOrderByDueDateAsc(statuses)
                : leadFollowupRepository.findByOrganizationIdAndStatusInOrderByDueDateAsc(organizationId, statuses);
    }

    private List<LeadFollowup> followupsDueByStatus(Long organizationId, List<FollowupStatus> statuses, LocalDate dueDate) {
        return organizationId == null
                ? leadFollowupRepository.findByStatusInAndDueDateLessThanEqualOrderByDueDateAsc(statuses, dueDate)
                : leadFollowupRepository.findByOrganizationIdAndStatusInAndDueDateLessThanEqualOrderByDueDateAsc(
                        organizationId,
                        statuses,
                        dueDate);
    }

    private void ensureMutable(LeadFollowup followup) {
        if (followup.getStatus() == FollowupStatus.COMPLETED || followup.getStatus() == FollowupStatus.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Completed or cancelled follow-ups cannot be modified");
        }
    }
}
