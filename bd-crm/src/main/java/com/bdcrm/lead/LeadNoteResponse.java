package com.bdcrm.lead;

import java.time.OffsetDateTime;

public record LeadNoteResponse(
        Long id,
        String body,
        Long authorId,
        String authorName,
        OffsetDateTime createdAt) {

    public static LeadNoteResponse from(LeadNote note) {
        return new LeadNoteResponse(
                note.getId(),
                note.getBody(),
                note.getAuthor().getId(),
                note.getAuthor().getFullName(),
                note.getCreatedAt());
    }
}
