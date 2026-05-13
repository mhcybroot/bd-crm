package com.bdcrm.template;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowupTemplateRepository extends JpaRepository<FollowupTemplate, Long> {

    @EntityGraph(attributePaths = {"steps"})
    List<FollowupTemplate> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"steps"})
    Optional<FollowupTemplate> findById(Long id);

    @EntityGraph(attributePaths = {"steps"})
    Optional<FollowupTemplate> findFirstByIsDefaultTrueAndActiveTrue();
}
