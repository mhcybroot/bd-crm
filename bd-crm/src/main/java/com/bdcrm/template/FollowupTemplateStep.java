package com.bdcrm.template;

import com.bdcrm.common.BaseEntity;
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
@Table(name = "lead_followup_template_steps")
public class FollowupTemplateStep extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private FollowupTemplate template;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Column(name = "day_offset", nullable = false)
    private int dayOffset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ContactChannel channel;

    @Column(length = 500)
    private String instructions;
}
