package com.bdcrm.template;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowupTemplateRepository extends JpaRepository<FollowupTemplate, Long> {

    @EntityGraph(attributePaths = {"steps"})
    List<FollowupTemplate> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"steps"})
    List<FollowupTemplate> findAllByOrganizationIdOrderByNameAsc(Long organizationId);

    @EntityGraph(attributePaths = {"steps"})
    Optional<FollowupTemplate> findById(Long id);

    @EntityGraph(attributePaths = {"steps"})
    Optional<FollowupTemplate> findByIdAndOrganizationId(Long id, Long organizationId);

    @EntityGraph(attributePaths = {"steps"})
    Optional<FollowupTemplate> findFirstByIsDefaultTrueAndActiveTrue();

    @EntityGraph(attributePaths = {"steps"})
    Optional<FollowupTemplate> findFirstByOrganizationIdAndIsDefaultTrueAndActiveTrue(Long organizationId);

    @EntityGraph(attributePaths = {"steps"})
    Optional<FollowupTemplate> findByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = {"steps"})
    Optional<FollowupTemplate> findByNameIgnoreCaseAndOrganizationId(String name, Long organizationId);
}
