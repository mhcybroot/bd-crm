package com.bdcrm.lead;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadActivityRepository extends JpaRepository<LeadActivity, Long> {

    @EntityGraph(attributePaths = "actor")
    List<LeadActivity> findByLeadIdOrderByCreatedAtDesc(Long leadId);
}
