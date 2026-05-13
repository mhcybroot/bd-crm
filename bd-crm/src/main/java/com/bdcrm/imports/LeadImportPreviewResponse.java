package com.bdcrm.imports;

import java.util.List;

public record LeadImportPreviewResponse(
        int totalRows,
        List<RowPreview> rows,
        List<String> warnings) {

    public record RowPreview(
            int rowNumber,
            String companyName,
            String contactName,
            String email,
            String phone,
            boolean duplicateSuspected) {
    }
}
