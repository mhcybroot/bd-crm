package com.bdcrm.attachment;

import com.bdcrm.audit.AuditEventService;
import com.bdcrm.auth.SecurityUtils;
import com.bdcrm.common.ApiException;
import com.bdcrm.config.CrmProperties;
import com.bdcrm.lead.Lead;
import com.bdcrm.lead.LeadActivityService;
import com.bdcrm.lead.LeadActivityType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRecordRepository attachmentRepository;
    private final DocumentRecordRepository documentRepository;
    private final SecurityUtils securityUtils;
    private final CrmProperties crmProperties;
    private final LeadActivityService leadActivityService;
    private final AuditEventService auditEventService;

    @Transactional(readOnly = true)
    public List<AttachmentUploadResponse> listForLead(Long leadId) {
        return attachmentRepository.findByLeadIdOrderByCreatedAtDesc(leadId).stream()
                .map(AttachmentUploadResponse::from)
                .toList();
    }

    @Transactional
    public AttachmentUploadResponse uploadToLead(Lead lead, MultipartFile file) {
        try {
            Path root = Path.of(crmProperties.getAttachments().getStorageRoot()).toAbsolutePath().normalize();
            Files.createDirectories(root);
            String storedName = UUID.randomUUID() + "-" + file.getOriginalFilename();
            Path storedPath = root.resolve(storedName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, storedPath, StandardCopyOption.REPLACE_EXISTING);
            }
            AttachmentRecord record = new AttachmentRecord();
            record.setLead(lead);
            record.setUploadedBy(securityUtils.currentUserEntity());
            record.setOriginalFileName(file.getOriginalFilename());
            record.setStoredFileName(storedName);
            record.setContentType(file.getContentType());
            record.setFileSize(file.getSize());
            record.setChecksum(checksum(storedPath));
            record.setStoragePath(storedPath.toString());
            record = attachmentRepository.save(record);
            leadActivityService.log(lead, record.getUploadedBy(), LeadActivityType.ATTACHMENT_ADDED, "Uploaded attachment " + file.getOriginalFilename());
            auditEventService.log(record.getUploadedBy(), "ATTACHMENT_UPLOADED", "ATTACHMENT", record.getId(), "Uploaded attachment", null);
            return AttachmentUploadResponse.from(record);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store uploaded file");
        }
    }

    @Transactional(readOnly = true)
    public Resource download(Long attachmentId) {
        AttachmentRecord attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attachment not found"));
        return new FileSystemResource(attachment.getStoragePath());
    }

    @Transactional
    public DocumentLifecycleResponse upsertDocument(Long attachmentId, DocumentLifecycleRequest request) {
        AttachmentRecord attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attachment not found"));
        if (attachment.getLead() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only lead attachments can be promoted to documents");
        }
        DocumentRecord document = documentRepository.findByAttachmentId(attachmentId).orElseGet(DocumentRecord::new);
        document.setAttachment(attachment);
        document.setLead(attachment.getLead());
        document.setTitle(request.title().trim());
        document.setStatus(request.status());
        document = documentRepository.save(document);
        leadActivityService.log(attachment.getLead(), securityUtils.currentUserEntity(), LeadActivityType.DOCUMENT_UPDATED,
                "Updated document " + document.getTitle() + " to " + document.getStatus());
        return DocumentLifecycleResponse.from(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentLifecycleResponse> documentsForLead(Long leadId) {
        return documentRepository.findByLeadIdOrderByCreatedAtDesc(leadId).stream()
                .map(DocumentLifecycleResponse::from)
                .toList();
    }

    private String checksum(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Files.readAllBytes(path));
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IOException("Unable to calculate checksum", exception);
        }
    }
}
