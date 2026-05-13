package com.bdcrm.imports;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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

    @PostMapping(value = "/leads/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LeadImportPreviewResponse preview(@RequestParam("file") MultipartFile file) throws IOException {
        return importService.preview(file);
    }

    @PostMapping(value = "/leads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LeadImportResultResponse importLeads(@RequestParam("file") MultipartFile file) throws IOException {
        return importService.importCsv(file);
    }
}
