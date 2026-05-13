package com.bdcrm.qualification;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadQualificationRepository extends JpaRepository<LeadQualification, Long> {

    @EntityGraph(attributePaths = {"updatedBy"})
    Optional<LeadQualification> findByLeadId(Long leadId);
}
