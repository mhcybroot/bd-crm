package com.bdcrm.imports;

import java.util.List;

public record LeadImportResultResponse(
        int createdCount,
        int updatedCount,
        int skippedCount,
        int duplicateCount,
        int invalidCount,
        List<String> errors,
        List<LeadImportRowResult> rowResults) {
}
