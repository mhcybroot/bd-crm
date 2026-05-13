package com.bdcrm.notification;

public record NotificationPreferenceRequest(
        boolean inAppEnabled,
        boolean emailEnabled) {
}
