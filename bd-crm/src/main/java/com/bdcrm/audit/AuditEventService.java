package com.bdcrm.audit;

import com.bdcrm.user.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;

    @Transactional
    public void log(User actor, String eventType, String entityType, Long entityId, String description, String detailsJson) {
        AuditEvent event = new AuditEvent();
        event.setActor(actor);
        event.setEventType(eventType);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setDescription(description);
        event.setDetailsJson(detailsJson);
        auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> latest() {
        return auditEventRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(AuditEventResponse::from)
                .toList();
    }
}
