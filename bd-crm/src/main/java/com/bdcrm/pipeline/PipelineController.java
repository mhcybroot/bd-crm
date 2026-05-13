package com.bdcrm.pipeline;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public PipelineBoardResponse board(@PathVariable Long templateId) {
        return pipelineService.board(templateId);
    }
}
