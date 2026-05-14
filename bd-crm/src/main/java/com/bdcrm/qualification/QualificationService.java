package com.bdcrm.qualification;

import com.bdcrm.auth.SecurityUtils;
import com.bdcrm.lead.Lead;
import com.bdcrm.lead.LeadActivityService;
import com.bdcrm.lead.LeadActivityType;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QualificationService {

    private final LeadQualificationRepository qualificationRepository;
    private final SecurityUtils securityUtils;
    private final LeadActivityService leadActivityService;

    @Transactional(readOnly = true)
    public LeadQualificationResponse getResponse(Lead lead) {
        return qualificationRepository.findByLeadId(lead.getId())
                .map(LeadQualificationResponse::from)
                .orElseGet(this::emptyResponse);
    }

    @Transactional
    public LeadQualification getOrCreateEntity(Lead lead) {
        return qualificationRepository.findByLeadId(lead.getId())
                .orElseGet(() -> {
                    LeadQualification qualification = new LeadQualification();
                    qualification.setLead(lead);
                    qualification.setOrganization(lead.getOrganization());
                    return qualificationRepository.save(qualification);
                });
    }

    @Transactional
    public LeadQualificationResponse update(Lead lead, LeadQualificationRequest request) {
        LeadQualification qualification = getOrCreateEntity(lead);
        qualification.setBudgetRange(request.budgetRange());
        qualification.setAuthorityLevel(request.authorityLevel());
        qualification.setNeedSummary(request.needSummary());
        qualification.setTimelineTarget(request.timelineTarget());
        qualification.setFitScore(request.fitScore());
        qualification.setEngagementScore(request.engagementScore());
        qualification.setTotalScore(Math.min(100, Math.round((request.fitScore() + request.engagementScore()) / 2f)));
        qualification.setQualificationNotes(request.qualificationNotes());
        qualification.setUpdatedBy(securityUtils.currentUserEntity());
        qualification.setQualificationUpdatedAt(OffsetDateTime.now());
        leadActivityService.log(lead, securityUtils.currentUserEntity(), LeadActivityType.QUALIFICATION_UPDATED, "Updated lead qualification");
        return LeadQualificationResponse.from(qualification);
    }

    private LeadQualificationResponse emptyResponse() {
        return new LeadQualificationResponse(
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                0,
                null,
                null,
                null,
                null);
    }
}
