package com.bdcrm.lead;

import com.bdcrm.common.BaseEntity;
import com.bdcrm.duplicate.DuplicateState;
import com.bdcrm.organization.Organization;
import com.bdcrm.pipeline.TemplatePipelineStage;
import com.bdcrm.template.FollowupTemplate;
import com.bdcrm.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "leads")
public class Lead extends BaseEntity {

    @Column(name = "company_name", nullable = false, length = 180)
    private String companyName;

    @Column(name = "contact_name", nullable = false, length = 120)
    private String contactName;

    @Column(length = 120)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(length = 120)
    private String source;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LeadStatus status = LeadStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LeadPriority priority = LeadPriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_stage_id")
    private TemplatePipelineStage currentStage;

    @Column(name = "merged_into_lead_id")
    private Long mergedIntoLeadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "duplicate_state", nullable = false, length = 32)
    private DuplicateState duplicateState = DuplicateState.CLEAR;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id", nullable = false)
    private User assignedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private FollowupTemplate template;
}
