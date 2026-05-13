package com.bdcrm.imports;

import java.util.Map;

public record LeadImportRequest(
        LeadImportMode importMode,
        Map<String, LeadImportTargetField> columnMappings) {
}
