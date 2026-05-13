package com.bdcrm.imports;

public record LeadImportValidationIssue(
        String field,
        String message) {
}
