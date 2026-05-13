package com.bdcrm.lead;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {

    @Override
    @EntityGraph(attributePaths = {"assignedUser", "template"})
    Page<Lead> findAll(org.springframework.data.jpa.domain.Specification<Lead> spec, Pageable pageable);
}
