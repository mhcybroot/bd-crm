package com.bdcrm.imports;

import com.bdcrm.auth.SecurityUtils;
import com.bdcrm.duplicate.DuplicateService;
import com.bdcrm.lead.LeadCreateRequest;
import com.bdcrm.lead.LeadPriority;
import com.bdcrm.lead.LeadService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final ImportJobRepository importJobRepository;
    private final LeadService leadService;
    private final SecurityUtils securityUtils;
    private final DuplicateService duplicateService;

    @Transactional
    public LeadImportPreviewResponse preview(MultipartFile file) throws IOException {
        List<LeadImportPreviewResponse.RowPreview> rows = parse(file).stream()
                .map(parsed -> new LeadImportPreviewResponse.RowPreview(
                        parsed.rowNumber(),
                        parsed.companyName(),
                        parsed.contactName(),
                        parsed.email(),
                        parsed.phone(),
                        false))
                .toList();
        return new LeadImportPreviewResponse(rows.size(), rows, List.of("CSV columns expected: companyName,contactName,email,phone,source"));
    }

    @Transactional
    public LeadImportResultResponse importCsv(MultipartFile file) throws IOException {
        List<String> errors = new ArrayList<>();
        int created = 0;
        for (ParsedRow row : parse(file)) {
            try {
                leadService.createLead(new LeadCreateRequest(
                        row.companyName(),
                        row.contactName(),
                        row.email(),
                        row.phone(),
                        row.source(),
                        null,
                        LeadPriority.MEDIUM,
                        null,
                        null));
                created++;
            } catch (Exception exception) {
                errors.add("Row " + row.rowNumber() + ": " + exception.getMessage());
            }
        }
        ImportJob job = new ImportJob();
        job.setRequestedBy(securityUtils.currentUserEntity());
        job.setFileName(file.getOriginalFilename());
        job.setStatus(errors.isEmpty() ? "COMPLETED" : "COMPLETED_WITH_ERRORS");
        job.setSummaryJson("{\"created\":" + created + ",\"errors\":" + errors.size() + "}");
        importJobRepository.save(job);
        duplicateService.rescan();
        return new LeadImportResultResponse(created, 0, errors);
    }

    private List<ParsedRow> parse(MultipartFile file) throws IOException {
        List<ParsedRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int rowNumber = 0;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (rowNumber == 1 && line.toLowerCase().contains("company")) {
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length < 2) {
                    continue;
                }
                rows.add(new ParsedRow(
                        rowNumber,
                        parts[0].trim(),
                        parts[1].trim(),
                        parts.length > 2 ? blankToNull(parts[2]) : null,
                        parts.length > 3 ? blankToNull(parts[3]) : null,
                        parts.length > 4 ? blankToNull(parts[4]) : null));
            }
        }
        return rows;
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }

    private record ParsedRow(
            int rowNumber,
            String companyName,
            String contactName,
            String email,
            String phone,
            String source) {
    }
}
