package com.bdcrm.savedview;

import com.bdcrm.common.BaseEntity;
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
@Table(name = "saved_views")
public class SavedView extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(name = "page_key", nullable = false, length = 64)
    private String pageKey;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private boolean shared;

    @Column(name = "config_json", nullable = false, columnDefinition = "text")
    private String configJson;
}
