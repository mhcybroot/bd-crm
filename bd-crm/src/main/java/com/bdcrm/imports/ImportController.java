package com.bdcrm.imports;

import com.bdcrm.common.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;
    private final ObjectMapper objectMapper;

    @GetMapping(value = "/leads/template", produces = "text/csv")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] bytes = importService.downloadTemplate().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("lead-import-template.csv").build().toString())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @PostMapping(value = "/leads/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LeadImportPreviewResponse preview(
            @RequestParam("file") MultipartFile file,
            @RequestParam("importMode") LeadImportMode importMode,
            @RequestParam("columnMappings") String columnMappings) throws IOException {
        return importService.preview(file, parseRequest(importMode, columnMappings));
    }

    @PostMapping(value = "/leads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LeadImportResultResponse importLeads(
            @RequestParam("file") MultipartFile file,
            @RequestParam("importMode") LeadImportMode importMode,
            @RequestParam("columnMappings") String columnMappings) throws IOException {
        return importService.importCsv(file, parseRequest(importMode, columnMappings));
    }

    private LeadImportRequest parseRequest(LeadImportMode importMode, String columnMappings) {
        try {
            Map<String, String> rawMappings = objectMapper.readValue(columnMappings, new TypeReference<>() {
            });
            return new LeadImportRequest(importMode, ImportService.parseMappings(rawMappings));
        } catch (Exception exception) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Column mappings payload is invalid");
        }
    }
}
