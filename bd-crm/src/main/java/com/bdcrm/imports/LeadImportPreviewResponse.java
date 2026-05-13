package com.bdcrm.imports;

import java.util.List;
import java.util.Map;

public record LeadImportPreviewResponse(
        List<String> detectedHeaders,
        List<String> requiredFields,
        Map<String, LeadImportTargetField> resolvedMapping,
        int totalRows,
        List<LeadImportPreviewRow> rows,
        List<String> warnings,
        LeadImportSummaryResponse summary,
        List<LeadImportTemplateField> fieldGuide) {
}
