package com.bdcrm.imports;

public record LeadImportTemplateField(
        String key,
        String label,
        boolean required,
        String formatHint,
        String example) {
}
