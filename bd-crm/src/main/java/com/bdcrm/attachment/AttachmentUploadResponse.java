package com.bdcrm.attachment;

import java.time.OffsetDateTime;

public record AttachmentUploadResponse(
        Long id,
        String originalFileName,
        String contentType,
        long fileSize,
        String checksum,
        Long uploadedByUserId,
        String uploadedByUserName,
        OffsetDateTime createdAt) {

    public static AttachmentUploadResponse from(AttachmentRecord record) {
        return new AttachmentUploadResponse(
                record.getId(),
                record.getOriginalFileName(),
                record.getContentType(),
                record.getFileSize(),
                record.getChecksum(),
                record.getUploadedBy().getId(),
                record.getUploadedBy().getFullName(),
                record.getCreatedAt());
    }
}
