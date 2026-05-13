package com.bdcrm.pipeline;

import java.util.List;

public record PipelineTemplateResponse(
        Long templateId,
        String templateName,
        List<StageDefinitionResponse> stages) {
}
