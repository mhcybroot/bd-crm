package com.bdcrm.pipeline;

import java.util.List;

public record PipelineBoardResponse(
        Long templateId,
        String templateName,
        List<PipelineBoardColumnResponse> columns) {
}
