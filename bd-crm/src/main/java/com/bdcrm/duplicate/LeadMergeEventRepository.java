package com.bdcrm.duplicate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadMergeEventRepository extends JpaRepository<LeadMergeEvent, Long> {
}
