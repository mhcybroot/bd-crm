package com.bdcrm.lead;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadNoteRepository extends JpaRepository<LeadNote, Long> {

    @EntityGraph(attributePaths = "author")
    List<LeadNote> findByLeadIdOrderByCreatedAtDesc(Long leadId);
}
