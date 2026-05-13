package com.bdcrm.attachment;

import com.bdcrm.lead.LeadService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final LeadService leadService;

    @GetMapping("/leads/{leadId}")
    public List<AttachmentUploadResponse> listForLead(@PathVariable Long leadId) {
        leadService.requireVisibleLeadEntity(leadId);
        return attachmentService.listForLead(leadId);
    }

    @PostMapping(value = "/leads/{leadId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AttachmentUploadResponse upload(@PathVariable Long leadId, @RequestParam("file") MultipartFile file) {
        return attachmentService.uploadToLead(leadService.requireVisibleLeadEntity(leadId), file);
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long attachmentId) {
        Resource resource = attachmentService.download(attachmentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(resource.getFilename()).build().toString())
                .body(resource);
    }

    @PutMapping("/{attachmentId}/document")
    public DocumentLifecycleResponse updateDocument(@PathVariable Long attachmentId, @Valid @RequestBody DocumentLifecycleRequest request) {
        return attachmentService.upsertDocument(attachmentId, request);
    }

    @GetMapping("/leads/{leadId}/documents")
    public List<DocumentLifecycleResponse> documents(@PathVariable Long leadId) {
        leadService.requireVisibleLeadEntity(leadId);
        return attachmentService.documentsForLead(leadId);
    }
}
