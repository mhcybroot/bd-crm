package com.bdcrm.lead;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LeadNoteRepository extends JpaRepository<LeadNote, Long>, JpaSpecificationExecutor<LeadNote> {

    @EntityGraph(attributePaths = "author")
    List<LeadNote> findByLeadIdOrderByCreatedAtDesc(Long leadId);
}