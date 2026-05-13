package com.bdcrm.communication;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadCommunicationRepository extends JpaRepository<LeadCommunication, Long> {

    @EntityGraph(attributePaths = {"actor"})
    List<LeadCommunication> findByLeadIdOrderByOccurredAtDesc(Long leadId);
}
