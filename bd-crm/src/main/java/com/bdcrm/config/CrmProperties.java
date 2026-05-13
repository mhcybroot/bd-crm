package com.bdcrm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crm")
public class CrmProperties {

    private Security security = new Security();
    private Escalation escalation = new Escalation();
    private Cors cors = new Cors();
    private Attachments attachments = new Attachments();
    private Notifications notifications = new Notifications();

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Escalation getEscalation() {
        return escalation;
    }

    public void setEscalation(Escalation escalation) {
        this.escalation = escalation;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Attachments getAttachments() {
        return attachments;
    }

    public void setAttachments(Attachments attachments) {
        this.attachments = attachments;
    }

    public Notifications getNotifications() {
        return notifications;
    }

    public void setNotifications(Notifications notifications) {
        this.notifications = notifications;
    }

    public static class Security {
        private String bootstrapPassword = "password";
        private String jwtSecret = "change-me-change-me-change-me-change-me";
        private long jwtExpirationMinutes = 720;

        public String getBootstrapPassword() {
            return bootstrapPassword;
        }

        public void setBootstrapPassword(String bootstrapPassword) {
            this.bootstrapPassword = bootstrapPassword;
        }

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public long getJwtExpirationMinutes() {
            return jwtExpirationMinutes;
        }

        public void setJwtExpirationMinutes(long jwtExpirationMinutes) {
            this.jwtExpirationMinutes = jwtExpirationMinutes;
        }
    }

    public static class Escalation {
        private int thresholdDays = 2;

        public int getThresholdDays() {
            return thresholdDays;
        }

        public void setThresholdDays(int thresholdDays) {
            this.thresholdDays = thresholdDays;
        }
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Attachments {
        private String storageRoot = "./storage";

        public String getStorageRoot() {
            return storageRoot;
        }

        public void setStorageRoot(String storageRoot) {
            this.storageRoot = storageRoot;
        }
    }

    public static class Notifications {
        private int dueSoonHours = 24;
        private boolean emailEnabled = false;
        private String fromAddress = "noreply@bdcrm.local";

        public int getDueSoonHours() {
            return dueSoonHours;
        }

        public void setDueSoonHours(int dueSoonHours) {
            this.dueSoonHours = dueSoonHours;
        }

        public boolean isEmailEnabled() {
            return emailEnabled;
        }

        public void setEmailEnabled(boolean emailEnabled) {
            this.emailEnabled = emailEnabled;
        }

        public String getFromAddress() {
            return fromAddress;
        }

        public void setFromAddress(String fromAddress) {
            this.fromAddress = fromAddress;
        }
    }
}
