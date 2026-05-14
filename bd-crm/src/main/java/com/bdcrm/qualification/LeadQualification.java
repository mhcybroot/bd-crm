package com.bdcrm.qualification;

import com.bdcrm.common.BaseEntity;
import com.bdcrm.lead.Lead;
import com.bdcrm.organization.Organization;
import com.bdcrm.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "lead_qualifications")
public class LeadQualification extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false, unique = true)
    private Lead lead;

    @Column(name = "budget_range", length = 120)
    private String budgetRange;

    @Column(name = "authority_level", length = 120)
    private String authorityLevel;

    @Column(name = "need_summary", columnDefinition = "text")
    private String needSummary;

    @Column(name = "timeline_target", length = 120)
    private String timelineTarget;

    @Column(name = "fit_score", nullable = false)
    private int fitScore;

    @Column(name = "engagement_score", nullable = false)
    private int engagementScore;

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    @Column(name = "qualification_notes", columnDefinition = "text")
    private String qualificationNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private User updatedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "qualification_updated_at")
    private OffsetDateTime qualificationUpdatedAt;
}
