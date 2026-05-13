package com.bdcrm.notification;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {

    @EntityGraph(attributePaths = {"lead", "followup"})
    List<NotificationEvent> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndTypeAndFollowupIdAndCreatedAtAfter(Long userId, String type, Long followupId, OffsetDateTime createdAt);
}
