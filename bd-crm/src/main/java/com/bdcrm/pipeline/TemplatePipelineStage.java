package com.bdcrm.pipeline;

import com.bdcrm.common.BaseEntity;
import com.bdcrm.template.FollowupTemplate;
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
@Table(name = "template_pipeline_stages")
public class TemplatePipelineStage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private FollowupTemplate template;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "stage_order", nullable = false)
    private int stageOrder;

    @Column(name = "sla_hours", nullable = false)
    private int slaHours = 72;

    @Column(name = "exit_automation", length = 120)
    private String exitAutomation;
}
