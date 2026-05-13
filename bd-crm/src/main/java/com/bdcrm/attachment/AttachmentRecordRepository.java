package com.bdcrm.attachment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AttachmentRecordRepository extends JpaRepository<AttachmentRecord, Long>, JpaSpecificationExecutor<AttachmentRecord> {

    @EntityGraph(attributePaths = {"uploadedBy"})
    List<AttachmentRecord> findByLeadIdOrderByCreatedAtDesc(Long leadId);

    @EntityGraph(attributePaths = {"uploadedBy", "lead", "note", "followup"})
    Optional<AttachmentRecord> findById(Long id);
}