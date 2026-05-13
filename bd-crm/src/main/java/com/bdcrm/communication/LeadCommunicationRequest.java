package com.bdcrm.communication;

import com.bdcrm.template.ContactChannel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record LeadCommunicationRequest(
        @NotNull ContactChannel channel,
        @Size(max = 255) String subject,
        String body,
        @Size(max = 64) String outcome,
        OffsetDateTime occurredAt) {
}
