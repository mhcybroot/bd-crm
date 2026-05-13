package com.bdcrm.duplicate;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DuplicateMatchRepository extends JpaRepository<DuplicateMatch, Long> {

    @EntityGraph(attributePaths = {"lead", "matchedLead", "reviewedBy"})
    List<DuplicateMatch> findAllByOrderByCreatedAtDesc();

    Optional<DuplicateMatch> findByLeadIdAndMatchedLeadId(Long leadId, Long matchedLeadId);
}
