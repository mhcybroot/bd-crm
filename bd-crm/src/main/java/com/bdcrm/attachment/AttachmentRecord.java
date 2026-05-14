package com.bdcrm.attachment;

import com.bdcrm.common.BaseEntity;
import com.bdcrm.followup.LeadFollowup;
import com.bdcrm.lead.Lead;
import com.bdcrm.lead.LeadNote;
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
@Table(name = "attachments")
public class AttachmentRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id")
    private LeadNote note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "followup_id")
    private LeadFollowup followup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private User uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false, length = 255)
    private String storedFileName;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(nullable = false, length = 120)
    private String checksum;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;
}
