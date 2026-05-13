package com.bdcrm.imports;

public record LeadImportSummaryResponse(
        int validRows,
        int warningRows,
        int invalidRows,
        int duplicateRows) {
}
