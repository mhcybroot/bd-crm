package com.bdcrm.attachment;

public record DocumentLifecycleResponse(
        Long id,
        Long attachmentId,
        String title,
        DocumentStatus status) {

    public static DocumentLifecycleResponse from(DocumentRecord record) {
        return new DocumentLifecycleResponse(record.getId(), record.getAttachment().getId(), record.getTitle(), record.getStatus());
    }
}
