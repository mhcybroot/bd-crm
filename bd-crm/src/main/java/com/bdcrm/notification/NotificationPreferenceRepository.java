package com.bdcrm.notification;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    Optional<NotificationPreference> findByUserId(Long userId);

    Optional<NotificationPreference> findByUserIdAndOrganizationId(Long userId, Long organizationId);
}
