package com.bdcrm.followup;

import com.bdcrm.common.BaseEntity;
import com.bdcrm.lead.Lead;
import com.bdcrm.organization.Organization;
import com.bdcrm.template.ContactChannel;
import com.bdcrm.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "lead_followups")
public class LeadFollowup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_user_id", nullable = false)
    private User assignedUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FollowupStatus status = FollowupStatus.DUE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ContactChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private FollowupOutcome outcome;

    @Column(length = 500)
    private String instructions;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "escalated_at")
    private OffsetDateTime escalatedAt;
}
