package com.bdcrm.pipeline;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadStageHistoryRepository extends JpaRepository<LeadStageHistory, Long> {

    @EntityGraph(attributePaths = {"stage", "changedBy"})
    List<LeadStageHistory> findByLeadIdOrderByEnteredAtDesc(Long leadId);

    boolean existsByStageId(Long stageId);
}
