package com.bdcrm.imports;

import java.util.List;
import java.util.Map;

public record LeadImportPreviewRow(
        int rowNumber,
        Map<String, String> values,
        List<LeadImportValidationIssue> issues,
        List<String> warnings,
        boolean duplicateSuspected,
        boolean valid,
        String suggestedAction) {
}
