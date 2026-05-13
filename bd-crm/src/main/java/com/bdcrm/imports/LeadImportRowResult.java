package com.bdcrm.imports;

import java.util.List;

public record LeadImportRowResult(
        int rowNumber,
        String outcome,
        List<String> messages) {
}
