package com.bdcrm.organization;

import com.bdcrm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "organizations")
public class Organization extends BaseEntity {

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 180)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    @Column(nullable = false, length = 80)
    private String timezone = "UTC";

    @Column(nullable = false, length = 16)
    private String locale = "en";

    @Column(name = "contact_email", nullable = false, length = 120)
    private String contactEmail;

    @Column(name = "plan_code", nullable = false, length = 64)
    private String planCode = "standard";

    @Column(name = "data_retention_days", nullable = false)
    private int dataRetentionDays = 365;
}
