package com.bdcrm.duplicate;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DuplicateMatchRepository extends JpaRepository<DuplicateMatch, Long> {

    @EntityGraph(attributePaths = {"lead", "matchedLead", "reviewedBy"})
    List<DuplicateMatch> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"lead", "matchedLead", "reviewedBy"})
    List<DuplicateMatch> findAllByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    @EntityGraph(attributePaths = {"lead", "matchedLead", "reviewedBy"})
    Optional<DuplicateMatch> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<DuplicateMatch> findByLeadIdAndMatchedLeadId(Long leadId, Long matchedLeadId);
}
