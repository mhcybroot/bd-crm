package com.bdcrm.pipeline;

import com.bdcrm.common.ApiException;
import com.bdcrm.lead.Lead;
import com.bdcrm.lead.LeadActivityService;
import com.bdcrm.lead.LeadActivityType;
import com.bdcrm.lead.LeadRepository;
import com.bdcrm.lead.LeadSummaryResponse;
import com.bdcrm.template.FollowupTemplate;
import com.bdcrm.template.FollowupTemplateRepository;
import com.bdcrm.user.User;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PipelineService {

    private final TemplatePipelineStageRepository stageRepository;
    private final LeadStageHistoryRepository historyRepository;
    private final LeadRepository leadRepository;
    private final FollowupTemplateRepository templateRepository;
    private final LeadActivityService leadActivityService;

    @Transactional(readOnly = true)
    public PipelineTemplateResponse forTemplate(Long templateId) {
        FollowupTemplate template = requireTemplate(templateId);
        return new PipelineTemplateResponse(
                template.getId(),
                template.getName(),
                stageRepository.findByTemplateIdOrderByStageOrderAsc(templateId).stream()
                        .map(StageDefinitionResponse::from)
                        .toList());
    }

    @Transactional(readOnly = true)
    public PipelineBoardResponse board(Long templateId) {
        FollowupTemplate template = requireTemplate(templateId);
        List<TemplatePipelineStage> stages = stageRepository.findByTemplateIdOrderByStageOrderAsc(templateId);
        List<Lead> leads = leadRepository.findAll().stream()
                .filter(lead -> lead.getTemplate().getId().equals(templateId))
                .filter(lead -> lead.getMergedIntoLeadId() == null)
                .toList();
        return new PipelineBoardResponse(
                template.getId(),
                template.getName(),
                stages.stream()
                        .map(stage -> new PipelineBoardColumnResponse(
                                stage.getId(),
                                stage.getName(),
                                stage.getSlaHours(),
                                leads.stream().filter(lead -> lead.getCurrentStage() != null && lead.getCurrentStage().getId().equals(stage.getId())).count(),
                                leads.stream()
                                        .filter(lead -> lead.getCurrentStage() != null && lead.getCurrentStage().getId().equals(stage.getId()))
                                        .sorted(Comparator.comparing(Lead::getUpdatedAt).reversed())
                                        .map(LeadSummaryResponse::from)
                                        .toList()))
                        .toList());
    }

    @Transactional
    public void syncTemplateStages(FollowupTemplate template, List<StageDefinitionRequest> requests) {
        Map<Integer, TemplatePipelineStage> existingByOrder = template.getPipelineStages().stream()
                .collect(LinkedHashMap::new, (map, stage) -> map.put(stage.getStageOrder(), stage), Map::putAll);
        List<TemplatePipelineStage> orderedStages = new ArrayList<>();
        for (StageDefinitionRequest request : requests.stream()
                .sorted(Comparator.comparingInt(StageDefinitionRequest::stageOrder))
                .toList()) {
            TemplatePipelineStage stage = existingByOrder.remove(request.stageOrder());
            if (stage == null) {
                stage = new TemplatePipelineStage();
                stage.setTemplate(template);
            }
            stage.setName(request.name().trim());
            stage.setStageOrder(request.stageOrder());
            stage.setSlaHours(request.slaHours());
            stage.setExitAutomation(request.exitAutomation());
            orderedStages.add(stage);
        }
        for (TemplatePipelineStage removedStage : existingByOrder.values()) {
            if (isStageReferenced(removedStage)) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot remove pipeline stage '" + removedStage.getName() + "' because it is already used by leads or stage history");
            }
        }
        template.getPipelineStages().clear();
        template.getPipelineStages().addAll(orderedStages);
    }

    @Transactional
    public void ensureLeadStage(Lead lead, User actor, String note) {
        TemplatePipelineStage currentStage = lead.getCurrentStage();
        if (currentStage != null && currentStage.getTemplate().getId().equals(lead.getTemplate().getId())) {
            return;
        }
        TemplatePipelineStage firstStage = stageRepository.findFirstByTemplateIdOrderByStageOrderAsc(lead.getTemplate().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Template has no pipeline stages"));
        moveLeadToStage(lead, firstStage, actor, note == null ? "Lead entered initial stage" : note);
    }

    @Transactional
    public void moveLeadToStage(Lead lead, TemplatePipelineStage stage, User actor, String note) {
        if (!stage.getTemplate().getId().equals(lead.getTemplate().getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Stage does not belong to lead template");
        }
        if (lead.getCurrentStage() != null && lead.getCurrentStage().getId().equals(stage.getId())) {
            return;
        }
        historyRepository.findByLeadIdOrderByEnteredAtDesc(lead.getId()).stream()
                .filter(history -> history.getExitedAt() == null)
                .findFirst()
                .ifPresent(history -> history.setExitedAt(OffsetDateTime.now()));
        lead.setCurrentStage(stage);
        LeadStageHistory history = new LeadStageHistory();
        history.setLead(lead);
        history.setStage(stage);
        history.setChangedBy(actor);
        history.setChangeNote(note);
        history.setEnteredAt(OffsetDateTime.now());
        historyRepository.save(history);
        leadActivityService.log(lead, actor, LeadActivityType.STAGE_CHANGED, "Moved lead to stage " + stage.getName());
    }

    @Transactional(readOnly = true)
    public List<LeadStageHistoryResponse> historyForLead(Long leadId) {
        return historyRepository.findByLeadIdOrderByEnteredAtDesc(leadId).stream()
                .map(LeadStageHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplatePipelineStage requireStage(Long stageId) {
        return stageRepository.findById(stageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Stage not found"));
    }

    private FollowupTemplate requireTemplate(Long templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Template not found"));
    }

    private boolean isStageReferenced(TemplatePipelineStage stage) {
        return stage.getId() != null
                && (leadRepository.existsByCurrentStageId(stage.getId())
                || historyRepository.existsByStageId(stage.getId()));
    }
}
