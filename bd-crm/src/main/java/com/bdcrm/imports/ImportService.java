package com.bdcrm.imports;

import com.bdcrm.auth.SecurityUtils;
import com.bdcrm.common.ApiException;
import com.bdcrm.duplicate.DuplicateService;
import com.bdcrm.lead.Lead;
import com.bdcrm.lead.LeadCreateRequest;
import com.bdcrm.lead.LeadPriority;
import com.bdcrm.lead.LeadRepository;
import com.bdcrm.lead.LeadService;
import com.bdcrm.lead.LeadUpdateRequest;
import com.bdcrm.template.FollowupTemplate;
import com.bdcrm.template.FollowupTemplateRepository;
import com.bdcrm.user.User;
import com.bdcrm.user.UserRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ImportService {

    private static final int PREVIEW_ROW_LIMIT = 25;
    private static final List<LeadImportTemplateField> FIELD_GUIDE = List.of(
            new LeadImportTemplateField("companyName", "Company name", true, "Text", "Acme Ltd"),
            new LeadImportTemplateField("contactName", "Contact name", true, "Text", "Amina Rahman"),
            new LeadImportTemplateField("email", "Email", false, "Valid email", "amina@acme.com"),
            new LeadImportTemplateField("phone", "Phone", false, "Text / phone", "+8801712345678"),
            new LeadImportTemplateField("source", "Lead source", false, "Text", "LinkedIn"),
            new LeadImportTemplateField("description", "Description", false, "Free text", "Warm intro from partner"),
            new LeadImportTemplateField("priority", "Priority", false, "LOW, MEDIUM, HIGH", "HIGH"),
            new LeadImportTemplateField("assignedUserId", "Assignee", false, "User id, username, or exact email", "bdrep"),
            new LeadImportTemplateField("templateId", "Follow-up template", false, "Template id or exact template name", "Standard 7 Touch"));

    private final ImportJobRepository importJobRepository;
    private final LeadService leadService;
    private final LeadRepository leadRepository;
    private final SecurityUtils securityUtils;
    private final DuplicateService duplicateService;
    private final UserRepository userRepository;
    private final FollowupTemplateRepository templateRepository;

    @Transactional(readOnly = true)
    public String downloadTemplate() {
        return "companyName,contactName,email,phone,source,description,priority,assignedUserId,templateId\n"
                + "\"Acme Ltd\",\"Amina Rahman\",\"amina@acme.com\",\"+8801712345678\",\"LinkedIn\",\"Warm intro from partner\",\"HIGH\",\"bdrep\",\"Standard 7 Touch\"\n";
    }

    @Transactional(readOnly = true)
    public LeadImportPreviewResponse preview(MultipartFile file, LeadImportRequest request) throws IOException {
        ParsedCsv csv = parse(file);
        Map<String, LeadImportTargetField> mapping = sanitizeMappings(csv.headers(), request.columnMappings());
        validateRequiredMappings(mapping);
        PreviewComputation preview = evaluate(csv, new LeadImportRequest(defaultMode(request.importMode()), mapping));
        return new LeadImportPreviewResponse(
                csv.headers(),
                List.of("companyName", "contactName"),
                mapping,
                preview.totalRows(),
                preview.rows(),
                preview.warnings(),
                preview.summary(),
                FIELD_GUIDE);
    }

    @Transactional
    public LeadImportResultResponse importCsv(MultipartFile file, LeadImportRequest request) throws IOException {
        ParsedCsv csv = parse(file);
        Map<String, LeadImportTargetField> mapping = sanitizeMappings(csv.headers(), request.columnMappings());
        validateRequiredMappings(mapping);
        PreviewComputation preview = evaluate(csv, new LeadImportRequest(defaultMode(request.importMode()), mapping));

        int created = 0;
        int updated = 0;
        int skipped = 0;
        int duplicateCount = 0;
        int invalidCount = 0;
        List<String> errors = new ArrayList<>();
        List<LeadImportRowResult> rowResults = new ArrayList<>();

        for (EvaluatedRow row : preview.evaluatedRows()) {
            if (!row.issues().isEmpty()) {
                invalidCount++;
                List<String> messages = row.issues().stream().map(LeadImportValidationIssue::message).toList();
                errors.add("Row " + row.rowNumber() + ": " + String.join(", ", messages));
                rowResults.add(new LeadImportRowResult(row.rowNumber(), "INVALID", messages));
                continue;
            }
            if (row.duplicateSuspected()) {
                duplicateCount++;
            }
            if ("SKIP_DUPLICATE".equals(row.suggestedAction())) {
                skipped++;
                List<String> messages = row.warnings().isEmpty() ? List.of("Skipped possible duplicate") : row.warnings();
                rowResults.add(new LeadImportRowResult(row.rowNumber(), "SKIPPED", messages));
                continue;
            }
            try {
                if ("UPDATE".equals(row.suggestedAction()) && row.matchedLead() != null) {
                    leadService.updateLead(row.matchedLead().getId(), new LeadUpdateRequest(
                            row.companyName(),
                            row.contactName(),
                            row.email(),
                            row.phone(),
                            row.source(),
                            row.description(),
                            row.priority(),
                            row.assignedUserId(),
                            row.templateId()));
                    updated++;
                    rowResults.add(new LeadImportRowResult(row.rowNumber(), "UPDATED", row.warnings()));
                } else {
                    leadService.createLead(new LeadCreateRequest(
                            row.companyName(),
                            row.contactName(),
                            row.email(),
                            row.phone(),
                            row.source(),
                            row.description(),
                            row.priority(),
                            row.assignedUserId(),
                            row.templateId()));
                    created++;
                    rowResults.add(new LeadImportRowResult(row.rowNumber(), "CREATED", row.warnings()));
                }
            } catch (Exception exception) {
                invalidCount++;
                errors.add("Row " + row.rowNumber() + ": " + exception.getMessage());
                rowResults.add(new LeadImportRowResult(row.rowNumber(), "ERROR", List.of(exception.getMessage())));
            }
        }

        ImportJob job = new ImportJob();
        job.setRequestedBy(securityUtils.currentUserEntity());
        job.setFileName(file.getOriginalFilename());
        job.setStatus(errors.isEmpty() ? "COMPLETED" : "COMPLETED_WITH_ERRORS");
        job.setSummaryJson("{\"created\":" + created
                + ",\"updated\":" + updated
                + ",\"skipped\":" + skipped
                + ",\"duplicateCount\":" + duplicateCount
                + ",\"invalidCount\":" + invalidCount + "}");
        importJobRepository.save(job);
        duplicateService.rescan();
        return new LeadImportResultResponse(created, updated, skipped, duplicateCount, invalidCount, errors, rowResults);
    }

    private PreviewComputation evaluate(ParsedCsv csv, LeadImportRequest request) {
        List<EvaluatedRow> evaluatedRows = csv.rows().stream()
                .map(row -> evaluateRow(row, request))
                .toList();

        List<LeadImportPreviewRow> previewRows = evaluatedRows.stream()
                .limit(PREVIEW_ROW_LIMIT)
                .map(row -> new LeadImportPreviewRow(
                        row.rowNumber(),
                        row.values(),
                        row.issues(),
                        row.warnings(),
                        row.duplicateSuspected(),
                        row.issues().isEmpty(),
                        row.suggestedAction()))
                .toList();

        int validRows = 0;
        int warningRows = 0;
        int invalidRows = 0;
        int duplicateRows = 0;
        for (EvaluatedRow row : evaluatedRows) {
            if (row.duplicateSuspected()) {
                duplicateRows++;
            }
            if (!row.issues().isEmpty()) {
                invalidRows++;
            } else if (!row.warnings().isEmpty()) {
                warningRows++;
            } else {
                validRows++;
            }
        }

        List<String> warnings = new ArrayList<>();
        if (evaluatedRows.size() > PREVIEW_ROW_LIMIT) {
            warnings.add("Preview limited to first " + PREVIEW_ROW_LIMIT + " rows while summary counts reflect the full file.");
        }
        warnings.add("Assignee accepts user ID, exact username, or exact email. Template accepts template ID or exact template name.");
        return new PreviewComputation(
                csv.rows().size(),
                previewRows,
                warnings,
                new LeadImportSummaryResponse(validRows, warningRows, invalidRows, duplicateRows),
                evaluatedRows);
    }

    private EvaluatedRow evaluateRow(CsvRow row, LeadImportRequest request) {
        Map<String, String> normalizedValues = new LinkedHashMap<>();
        List<LeadImportValidationIssue> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        EnumMap<LeadImportTargetField, String> targetValues = new EnumMap<>(LeadImportTargetField.class);

        request.columnMappings().forEach((header, targetField) -> {
            if (targetField == null || targetField == LeadImportTargetField.IGNORE) {
                return;
            }
            targetValues.put(targetField, normalizeOptional(row.valuesByHeader().get(header)));
        });

        String companyName = requireValue(targetValues.get(LeadImportTargetField.COMPANY_NAME), "companyName", "Company name is required", issues);
        String contactName = requireValue(targetValues.get(LeadImportTargetField.CONTACT_NAME), "contactName", "Contact name is required", issues);
        String email = normalizeOptional(targetValues.get(LeadImportTargetField.EMAIL));
        String phone = normalizeOptional(targetValues.get(LeadImportTargetField.PHONE));
        String source = normalizeOptional(targetValues.get(LeadImportTargetField.SOURCE));
        String description = normalizeOptional(targetValues.get(LeadImportTargetField.DESCRIPTION));
        LeadPriority priority = resolvePriority(targetValues.get(LeadImportTargetField.PRIORITY), issues);
        User assignee = resolveAssignee(targetValues.get(LeadImportTargetField.ASSIGNED_USER_ID), issues);
        FollowupTemplate template = resolveTemplate(targetValues.get(LeadImportTargetField.TEMPLATE_ID), issues);

        if (email != null && !isValidEmail(email)) {
            issues.add(new LeadImportValidationIssue("email", "Email must be valid"));
        }

        normalizedValues.put("companyName", companyName);
        normalizedValues.put("contactName", contactName);
        normalizedValues.put("email", email);
        normalizedValues.put("phone", phone);
        normalizedValues.put("source", source);
        normalizedValues.put("description", description);
        normalizedValues.put("priority", priority.name());
        normalizedValues.put("assignedUserId", assignee != null ? String.valueOf(assignee.getId()) : null);
        normalizedValues.put("templateId", template != null ? String.valueOf(template.getId()) : null);

        List<Lead> matches = findMatches(email, phone);
        boolean duplicateSuspected = !matches.isEmpty();
        Lead matchedLead = null;
        String suggestedAction = "CREATE";
        if (request.importMode() == LeadImportMode.CREATE_ONLY) {
            if (duplicateSuspected) {
                warnings.add("Possible duplicate found by matching email or phone.");
                suggestedAction = "SKIP_DUPLICATE";
            }
        } else if (duplicateSuspected) {
            if (matches.size() > 1) {
                issues.add(new LeadImportValidationIssue("match", "Multiple matching leads found for this row"));
                suggestedAction = "INVALID";
            } else {
                matchedLead = matches.getFirst();
                warnings.add("Existing lead will be updated.");
                suggestedAction = "UPDATE";
            }
        }

        return new EvaluatedRow(
                row.rowNumber(),
                normalizedValues,
                issues,
                warnings,
                duplicateSuspected,
                suggestedAction,
                matchedLead,
                companyName,
                contactName,
                email,
                phone,
                source,
                description,
                priority,
                assignee != null ? assignee.getId() : null,
                template != null ? template.getId() : null);
    }

    private Map<String, LeadImportTargetField> sanitizeMappings(List<String> headers, Map<String, LeadImportTargetField> requestedMappings) {
        if (requestedMappings == null || requestedMappings.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Column mappings are required");
        }
        Map<String, LeadImportTargetField> sanitized = new LinkedHashMap<>();
        for (String header : headers) {
            LeadImportTargetField targetField = requestedMappings.get(header);
            if (targetField != null) {
                sanitized.put(header, targetField);
            }
        }
        if (sanitized.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No valid column mappings were provided");
        }
        return sanitized;
    }

    private void validateRequiredMappings(Map<String, LeadImportTargetField> mapping) {
        Set<LeadImportTargetField> targets = new LinkedHashSet<>(mapping.values());
        if (!targets.contains(LeadImportTargetField.COMPANY_NAME)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mapping for companyName is required");
        }
        if (!targets.contains(LeadImportTargetField.CONTACT_NAME)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mapping for contactName is required");
        }
    }

    private ParsedCsv parse(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "CSV file is empty");
            }
            List<String> headers = parseCsvLine(headerLine).stream()
                    .map(String::trim)
                    .toList();
            if (headers.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "CSV header row is empty");
            }
            List<CsvRow> rows = new ArrayList<>();
            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }
                List<String> cells = parseCsvLine(line);
                Map<String, String> valuesByHeader = new LinkedHashMap<>();
                for (int index = 0; index < headers.size(); index++) {
                    valuesByHeader.put(headers.get(index), index < cells.size() ? cells.get(index).trim() : null);
                }
                rows.add(new CsvRow(rowNumber, valuesByHeader));
            }
            return new ParsedCsv(headers, rows);
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    private String requireValue(String value, String field, String message, List<LeadImportValidationIssue> issues) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            issues.add(new LeadImportValidationIssue(field, message));
        }
        return normalized;
    }

    private LeadPriority resolvePriority(String value, List<LeadImportValidationIssue> issues) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return LeadPriority.MEDIUM;
        }
        try {
            return LeadPriority.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            issues.add(new LeadImportValidationIssue("priority", "Priority must be one of LOW, MEDIUM, HIGH"));
            return LeadPriority.MEDIUM;
        }
    }

    private User resolveAssignee(String value, List<LeadImportValidationIssue> issues) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        Optional<User> byId = normalized.chars().allMatch(Character::isDigit)
                ? userRepository.findById(Long.parseLong(normalized))
                : Optional.empty();
        Optional<User> byUsername = byId.isPresent() ? byId : userRepository.findByUsernameIgnoreCase(normalized);
        Optional<User> resolved = byUsername.isPresent() ? byUsername : userRepository.findByEmailIgnoreCase(normalized);
        if (resolved.isEmpty()) {
            issues.add(new LeadImportValidationIssue("assignedUserId", "Assignee could not be resolved"));
            return null;
        }
        return resolved.get();
    }

    private FollowupTemplate resolveTemplate(String value, List<LeadImportValidationIssue> issues) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        Optional<FollowupTemplate> byId = normalized.chars().allMatch(Character::isDigit)
                ? templateRepository.findById(Long.parseLong(normalized))
                : Optional.empty();
        Optional<FollowupTemplate> resolved = byId.isPresent() ? byId : templateRepository.findByNameIgnoreCase(normalized);
        if (resolved.isEmpty()) {
            issues.add(new LeadImportValidationIssue("templateId", "Follow-up template could not be resolved"));
            return null;
        }
        return resolved.get();
    }

    private List<Lead> findMatches(String email, String phone) {
        Map<Long, Lead> matches = new LinkedHashMap<>();
        if (email != null) {
            leadRepository.findAllByEmailIgnoreCase(email).forEach(lead -> matches.put(lead.getId(), lead));
        }
        if (phone != null) {
            leadRepository.findAllByPhoneIgnoreCase(phone).forEach(lead -> matches.put(lead.getId(), lead));
        }
        return matches.values().stream()
                .filter(lead -> lead.getMergedIntoLeadId() == null)
                .toList();
    }

    private boolean isValidEmail(String email) {
        return email.contains("@") && email.indexOf('@') > 0 && email.indexOf('@') < email.length() - 1;
    }

    private String normalizeOptional(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }

    private LeadImportMode defaultMode(LeadImportMode mode) {
        return mode == null ? LeadImportMode.CREATE_ONLY : mode;
    }

    public static Map<String, LeadImportTargetField> parseMappings(Map<String, String> rawMappings) {
        if (rawMappings == null) {
            return Map.of();
        }
        return rawMappings.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> toTargetField(entry.getValue()),
                        (left, right) -> right,
                        LinkedHashMap::new));
    }

    private static LeadImportTargetField toTargetField(String value) {
        return switch (value.trim()) {
            case "companyName" -> LeadImportTargetField.COMPANY_NAME;
            case "contactName" -> LeadImportTargetField.CONTACT_NAME;
            case "email" -> LeadImportTargetField.EMAIL;
            case "phone" -> LeadImportTargetField.PHONE;
            case "source" -> LeadImportTargetField.SOURCE;
            case "description" -> LeadImportTargetField.DESCRIPTION;
            case "priority" -> LeadImportTargetField.PRIORITY;
            case "assignedUserId" -> LeadImportTargetField.ASSIGNED_USER_ID;
            case "templateId" -> LeadImportTargetField.TEMPLATE_ID;
            case "IGNORE", "ignore" -> LeadImportTargetField.IGNORE;
            default -> LeadImportTargetField.valueOf(value.trim().toUpperCase(Locale.ROOT));
        };
    }

    private record ParsedCsv(
            List<String> headers,
            List<CsvRow> rows) {
    }

    private record CsvRow(
            int rowNumber,
            Map<String, String> valuesByHeader) {
    }

    private record PreviewComputation(
            int totalRows,
            List<LeadImportPreviewRow> rows,
            List<String> warnings,
            LeadImportSummaryResponse summary,
            List<EvaluatedRow> evaluatedRows) {
    }

    private record EvaluatedRow(
            int rowNumber,
            Map<String, String> values,
            List<LeadImportValidationIssue> issues,
            List<String> warnings,
            boolean duplicateSuspected,
            String suggestedAction,
            Lead matchedLead,
            String companyName,
            String contactName,
            String email,
            String phone,
            String source,
            String description,
            LeadPriority priority,
            Long assignedUserId,
            Long templateId) {
    }
}
