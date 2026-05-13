package com.bdcrm.notification;

public record NotificationPreferenceResponse(
        boolean inAppEnabled,
        boolean emailEnabled) {

    public static NotificationPreferenceResponse from(NotificationPreference preference) {
        return new NotificationPreferenceResponse(preference.isInAppEnabled(), preference.isEmailEnabled());
    }
}
