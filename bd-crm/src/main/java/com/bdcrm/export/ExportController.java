package com.bdcrm.export;

import com.bdcrm.reporting.ReportFilterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/leads")
    public ResponseEntity<String> leads() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leads.csv")
                .contentType(new MediaType("text", "csv"))
                .body(exportService.leadsCsv());
    }

    @GetMapping("/reports")
    public ResponseEntity<String> reports(@ModelAttribute ReportFilterRequest filter) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.csv")
                .contentType(new MediaType("text", "csv"))
                .body(exportService.reportCsv(filter));
    }
}
