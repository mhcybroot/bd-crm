package com.bdcrm.attachment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRecordRepository extends JpaRepository<DocumentRecord, Long> {

    @EntityGraph(attributePaths = {"attachment"})
    List<DocumentRecord> findByLeadIdOrderByCreatedAtDesc(Long leadId);

    @EntityGraph(attributePaths = {"attachment"})
    Optional<DocumentRecord> findByAttachmentId(Long attachmentId);
}
