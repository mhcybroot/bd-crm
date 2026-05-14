package com.bdcrm.lead;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LeadActivityRepository extends JpaRepository<LeadActivity, Long>, JpaSpecificationExecutor<LeadActivity> {

    @EntityGraph(attributePaths = "actor")
    List<LeadActivity> findByLeadIdOrderByCreatedAtDesc(Long leadId);
}