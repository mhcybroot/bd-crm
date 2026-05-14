package com.bdcrm.communication;

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
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "lead_communications")
public class LeadCommunication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ContactChannel channel;

    @Column(length = 255)
    private String subject;

    @Column(columnDefinition = "text")
    private String body;

    @Column(length = 64)
    private String outcome;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;
}
