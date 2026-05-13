package com.bdcrm.pipeline;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplatePipelineStageRepository extends JpaRepository<TemplatePipelineStage, Long> {

    @EntityGraph(attributePaths = {"template"})
    List<TemplatePipelineStage> findByTemplateIdOrderByStageOrderAsc(Long templateId);

    @EntityGraph(attributePaths = {"template"})
    Optional<TemplatePipelineStage> findById(Long id);

    Optional<TemplatePipelineStage> findFirstByTemplateIdOrderByStageOrderAsc(Long templateId);

    void deleteByTemplateId(Long templateId);
}
