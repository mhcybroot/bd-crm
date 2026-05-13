package com.bdcrm.communication;

import com.bdcrm.template.ContactChannel;
import java.time.OffsetDateTime;

public record LeadCommunicationResponse(
        Long id,
        ContactChannel channel,
        String subject,
        String body,
        String outcome,
        OffsetDateTime occurredAt,
        Long actorId,
        String actorName) {

    public static LeadCommunicationResponse from(LeadCommunication communication) {
        return new LeadCommunicationResponse(
                communication.getId(),
                communication.getChannel(),
                communication.getSubject(),
                communication.getBody(),
                communication.getOutcome(),
                communication.getOccurredAt(),
                communication.getActor().getId(),
                communication.getActor().getFullName());
    }
}
