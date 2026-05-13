package com.bdcrm.notification;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> list() {
        return notificationService.listForCurrentUser();
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse read(@PathVariable Long notificationId) {
        return notificationService.markRead(notificationId);
    }

    @GetMapping("/preferences")
    public NotificationPreferenceResponse preferences() {
        return notificationService.currentPreference();
    }

    @PutMapping("/preferences")
    public NotificationPreferenceResponse updatePreferences(@Valid @RequestBody NotificationPreferenceRequest request) {
        return notificationService.updatePreference(request);
    }
}
