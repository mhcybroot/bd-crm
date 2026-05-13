package com.bdcrm.followup;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EscalationEventRepository extends JpaRepository<EscalationEvent, Long> {

    @EntityGraph(attributePaths = {"followup", "lead", "escalatedToUser"})
    List<EscalationEvent> findByLeadIdOrderByCreatedAtDesc(Long leadId);

    boolean existsByFollowupId(Long followupId);
}
