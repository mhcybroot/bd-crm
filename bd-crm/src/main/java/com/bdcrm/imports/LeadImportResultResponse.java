package com.bdcrm.imports;

import java.util.List;

public record LeadImportResultResponse(
        int createdCount,
        int duplicateCount,
        List<String> errors) {
}
