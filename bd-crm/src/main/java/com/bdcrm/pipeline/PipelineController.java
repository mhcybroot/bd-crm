package com.bdcrm.pipeline;

import com.bdcrm.lead.LeadPriority;
import com.bdcrm.lead.LeadStatus;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pipelines")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineService pipelineService;

    @GetMapping("/templates/{templateId}")
    public PipelineTemplateResponse forTemplate(@PathVariable Long templateId) {
        return pipelineService.forTemplate(templateId);
    }

    @GetMapping("/templates/{templateId}/board")
    public PipelineBoardResponse board(
            @PathVariable Long templateId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long assignedUserId,
            @RequestParam(required = false) LeadPriority priority,
            @RequestParam(required = false) LeadStatus leadStatus,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo) {
        return pipelineService.board(templateId, new PipelineBoardFilterRequest(
                search, assignedUserId, priority, leadStatus, source, dateFrom, dateTo));
    }

    @GetMapping("/templates/{templateId}/board/stages/{stageId}/leads")
    public PipelineStageLeadPageResponse stageLeads(
            @PathVariable Long templateId,
            @PathVariable Long stageId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long assignedUserId,
            @RequestParam(required = false) LeadPriority priority,
            @RequestParam(required = false) LeadStatus leadStatus,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return pipelineService.stageLeads(
                templateId,
                stageId,
                new PipelineBoardFilterRequest(search, assignedUserId, priority, leadStatus, source, dateFrom, dateTo),
                page,
                size);
    }
}
