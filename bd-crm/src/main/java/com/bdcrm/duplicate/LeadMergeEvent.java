package com.bdcrm.duplicate;

import com.bdcrm.common.BaseEntity;
import com.bdcrm.lead.Lead;
import com.bdcrm.organization.Organization;
import com.bdcrm.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "lead_merge_events")
public class LeadMergeEvent extends BaseEntity {

    @Column(name = "source_lead_id", nullable = false)
    private Long sourceLeadId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_lead_id", nullable = false)
    private Lead targetLead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_by_user_id", nullable = false)
    private User mergedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;
}
