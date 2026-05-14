package com.bdcrm.lead;

import com.bdcrm.auth.SecurityUtils;
import com.bdcrm.attachment.AttachmentService;
import com.bdcrm.common.ApiException;
import com.bdcrm.common.PagedResponse;
import com.bdcrm.communication.CommunicationService;
import com.bdcrm.duplicate.DuplicateState;
import com.bdcrm.followup.FollowupService;
import com.bdcrm.followup.LeadFollowupResponse;
import com.bdcrm.pipeline.LeadStageUpdateRequest;
import com.bdcrm.pipeline.PipelineService;
import com.bdcrm.qualification.LeadQualificationResponse;
import com.bdcrm.qualification.QualificationService;
import com.bdcrm.template.FollowupTemplate;
import com.bdcrm.template.TemplateService;
import com.bdcrm.user.User;
import com.bdcrm.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadNoteRepository leadNoteRepository;
    private final LeadActivityRepository leadActivityRepository;
    private final UserRepository userRepository;
    private final TemplateService templateService;
    private final FollowupService followupService;
    private final LeadActivityService leadActivityService;
    private final SecurityUtils securityUtils;
    private final QualificationService qualificationService;
    private final PipelineService pipelineService;
    private final CommunicationService communicationService;
    private final AttachmentService attachmentService;

    @Transactional
    public LeadDetailResponse createLead(LeadCreateRequest request) {
        User actor = securityUtils.currentUserEntity();
        User assignedUser = request.assignedUserId() == null ? actor : requireUser(request.assignedUserId());
        FollowupTemplate template = request.templateId() == null
                ? templateService.getDefaultTemplateEntity()
                : templateService.getTemplateEntity(request.templateId());

        Lead lead = new Lead();
        apply(lead, request.companyName(), request.contactName(), request.email(), request.phone(), request.source(),
                request.description(), request.priority(), assignedUser, template);
        lead.setOrganization(actor.getOrganization());
        lead = leadRepository.save(lead);
        followupService.syncFromTemplate(lead);
        qualificationService.getOrCreateEntity(lead);
        pipelineService.ensureLeadStage(lead, actor, "Lead created");
        leadActivityService.log(lead, actor, LeadActivityType.LEAD_CREATED, "Lead created");
        return getLead(lead.getId());
    }

    @Transactional
    public LeadDetailResponse updateLead(Long leadId, LeadUpdateRequest request) {
        Lead lead = requireVisibleLead(leadId);
        FollowupTemplate template = request.templateId() == null
                ? lead.getTemplate()
                : templateService.getTemplateEntity(request.templateId());
        User assignedUser = request.assignedUserId() == null ? lead.getAssignedUser() : requireUser(request.assignedUserId());
        boolean templateChanged = !template.getId().equals(lead.getTemplate().getId());
        boolean ownerChanged = !assignedUser.getId().equals(lead.getAssignedUser().getId());
        apply(lead, request.companyName(), request.contactName(), request.email(), request.phone(), request.source(),
                request.description(), request.priority(), assignedUser, template);
        if (templateChanged || ownerChanged) {
            followupService.syncFromTemplate(lead);
            pipelineService.ensureLeadStage(lead, securityUtils.currentUserEntity(), "Lead updated to template pipeline");
        }
        leadActivityService.log(lead, securityUtils.currentUserEntity(), LeadActivityType.LEAD_UPDATED, "Lead updated");
        return getLead(lead.getId());
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeadSummaryResponse> listLeads(
            LeadStatus status,
            Long assignedUserId,
            String search,
            int page,
            int size) {
        User currentUser = securityUtils.currentUserEntity();
        Long effectiveAssignedUserId = securityUtils.hasAnyRole("PLATFORM_ADMIN", "ORG_ADMIN", "ORG_MANAGER")
                ? assignedUserId
                : currentUser.getId();
        Long effectiveOrganizationId = securityUtils.hasPlatformRole("PLATFORM_ADMIN") ? null : currentUser.getOrganization().getId();
        Specification<Lead> spec = Specification.where(LeadSpecifications.organizationId(effectiveOrganizationId))
                .and(LeadSpecifications.hasStatus(status))
                .and(LeadSpecifications.assignedTo(effectiveAssignedUserId))
                .and(LeadSpecifications.search(search));
        Page<Lead> result = leadRepository.findAll(spec, PageRequest.of(page, size, Sort.by("updatedAt").descending()));
        return new PagedResponse<>(
                result.getContent().stream().map(LeadSummaryResponse::from).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                page,
                size);
    }

    @Transactional(readOnly = true)
    public LeadDetailResponse getLead(Long leadId) {
        Lead lead = requireVisibleLead(leadId);
        List<LeadFollowupResponse> followups = followupService.findForLead(leadId);
        List<LeadNoteResponse> notes = leadNoteRepository.findByLeadIdOrderByCreatedAtDesc(leadId).stream()
                .map(LeadNoteResponse::from)
                .toList();
        List<LeadActivityResponse> activities = leadActivityRepository.findByLeadIdOrderByCreatedAtDesc(leadId).stream()
                .map(LeadActivityResponse::from)
                .toList();
        LeadQualificationResponse qualification = qualificationService.getResponse(lead);
        return new LeadDetailResponse(
                LeadSummaryResponse.from(lead),
                followups,
                notes,
                activities,
                qualification,
                new LeadScoreSummaryResponse(qualification.fitScore(), qualification.engagementScore(), qualification.totalScore()),
                pipelineService.historyForLead(leadId),
                communicationService.listForLead(leadId),
                attachmentService.listForLead(leadId),
                attachmentService.documentsForLead(leadId));
    }

    @Transactional
    public LeadDetailResponse assignLead(Long leadId, LeadAssignmentRequest request) {
        Lead lead = requireVisibleLead(leadId);
        User assignee = requireUser(request.assignedUserId());
        lead.setAssignedUser(assignee);
        followupService.syncFromTemplate(lead);
        leadActivityService.log(lead, securityUtils.currentUserEntity(), LeadActivityType.ASSIGNED,
                "Assigned lead to " + assignee.getFullName());
        return getLead(lead.getId());
    }

    @Transactional
    public LeadDetailResponse updateStatus(Long leadId, LeadStatusUpdateRequest request) {
        Lead lead = requireVisibleLead(leadId);
        lead.setStatus(request.status());
        User actor = securityUtils.currentUserEntity();
        leadActivityService.log(lead, actor, LeadActivityType.STATUS_CHANGED, "Lead status changed to " + request.status());
        if (request.status() == LeadStatus.WON || request.status() == LeadStatus.LOST) {
            followupService.closeOpenFollowups(lead, actor, "Closed open follow-ups after lead moved to " + request.status());
        }
        return getLead(leadId);
    }

    @Transactional
    public LeadDetailResponse updateStage(Long leadId, LeadStageUpdateRequest request) {
        Lead lead = requireVisibleLead(leadId);
        pipelineService.moveLeadToStage(lead, pipelineService.requireStage(request.stageId()), securityUtils.currentUserEntity(), request.note());
        return getLead(leadId);
    }

    @Transactional
    public LeadNoteResponse addNote(Long leadId, LeadNoteRequest request) {
        Lead lead = requireVisibleLead(leadId);
        User actor = securityUtils.currentUserEntity();
        LeadNote note = new LeadNote();
        note.setLead(lead);
        note.setAuthor(actor);
        note.setOrganization(lead.getOrganization());
        note.setBody(request.body().trim());
        note = leadNoteRepository.save(note);
        leadActivityService.log(lead, actor, LeadActivityType.NOTE_ADDED, "Added a note");
        return LeadNoteResponse.from(note);
    }

    private void apply(
            Lead lead,
            String companyName,
            String contactName,
            String email,
            String phone,
            String source,
            String description,
            LeadPriority priority,
            User assignedUser,
            FollowupTemplate template) {
        lead.setCompanyName(companyName.trim());
        lead.setContactName(contactName.trim());
        lead.setEmail(email);
        lead.setPhone(phone);
        lead.setSource(source);
        lead.setDescription(description);
        lead.setPriority(priority == null ? LeadPriority.MEDIUM : priority);
        lead.setAssignedUser(assignedUser);
        lead.setTemplate(template);
        if (lead.getOrganization() == null) {
            lead.setOrganization(assignedUser.getOrganization());
        }
        if (lead.getDuplicateState() == null) {
            lead.setDuplicateState(DuplicateState.CLEAR);
        }
    }

    public Lead requireVisibleLeadEntity(Long leadId) {
        return requireVisibleLead(leadId);
    }

    Lead requireVisibleLead(Long leadId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Lead not found"));
        User currentUser = securityUtils.currentUserEntity();
        if (securityUtils.hasPlatformRole("PLATFORM_ADMIN")) {
            return lead;
        }
        if (!lead.getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this lead");
        }
        if (securityUtils.hasAnyRole("ORG_ADMIN", "ORG_MANAGER") || lead.getAssignedUser().getId().equals(currentUser.getId())) {
            return lead;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this lead");
    }

    private User requireUser(Long userId) {
        if (securityUtils.hasPlatformRole("PLATFORM_ADMIN")) {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        }
        Long organizationId = securityUtils.currentOrganizationId();
        return userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional
    public List<LeadSummaryResponse> bulkAction(BulkLeadActionRequest request) {
        User actor = securityUtils.currentUserEntity();
        List<Lead> leads = request.leadIds().stream().map(this::requireVisibleLead).toList();
        for (Lead lead : leads) {
            if (request.assignedUserId() != null) {
                lead.setAssignedUser(requireUser(request.assignedUserId()));
            }
            if (request.status() != null) {
                lead.setStatus(request.status());
            }
            if (request.stageId() != null) {
                pipelineService.moveLeadToStage(lead, pipelineService.requireStage(request.stageId()), actor, "Bulk lead action");
            }
            leadActivityService.log(lead, actor, LeadActivityType.LEAD_UPDATED, "Bulk lead action applied");
        }
        return leads.stream().map(LeadSummaryResponse::from).toList();
    }
}
