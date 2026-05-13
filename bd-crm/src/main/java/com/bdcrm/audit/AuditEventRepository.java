package com.bdcrm.audit;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @EntityGraph(attributePaths = {"actor"})
    List<AuditEvent> findTop100ByOrderByCreatedAtDesc();
}
