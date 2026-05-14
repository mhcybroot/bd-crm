package com.bdcrm.lead;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {

    @Override
    @EntityGraph(attributePaths = {"assignedUser", "template", "currentStage"})
    Page<Lead> findAll(org.springframework.data.jpa.domain.Specification<Lead> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"assignedUser", "template", "currentStage"})
    java.util.Optional<Lead> findById(Long id);

    boolean existsByCurrentStageId(Long currentStageId);

    @EntityGraph(attributePaths = {"assignedUser", "template", "currentStage"})
    java.util.List<Lead> findAllByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"assignedUser", "template", "currentStage"})
    java.util.List<Lead> findAllByEmailIgnoreCaseAndOrganizationId(String email, Long organizationId);

    @EntityGraph(attributePaths = {"assignedUser", "template", "currentStage"})
    java.util.List<Lead> findAllByPhoneIgnoreCase(String phone);

    @EntityGraph(attributePaths = {"assignedUser", "template", "currentStage"})
    java.util.List<Lead> findAllByPhoneIgnoreCaseAndOrganizationId(String phone, Long organizationId);
}
