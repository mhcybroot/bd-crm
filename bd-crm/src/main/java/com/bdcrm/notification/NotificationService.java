package com.bdcrm.notification;

import com.bdcrm.auth.SecurityUtils;
import com.bdcrm.config.CrmProperties;
import com.bdcrm.followup.FollowupStatus;
import com.bdcrm.followup.LeadFollowup;
import com.bdcrm.followup.LeadFollowupRepository;
import com.bdcrm.user.User;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationEventRepository notificationEventRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final SecurityUtils securityUtils;
    private final LeadFollowupRepository followupRepository;
    private final CrmProperties crmProperties;
    private final org.springframework.beans.factory.ObjectProvider<JavaMailSender> mailSenderProvider;

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForCurrentUser() {
        return notificationEventRepository.findByUserIdOrderByCreatedAtDesc(securityUtils.currentUserEntity().getId()).stream()
                .filter(event -> securityUtils.hasPlatformRole("PLATFORM_ADMIN")
                        || event.getOrganization().getId().equals(securityUtils.currentOrganizationId()))
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId) {
        NotificationEvent event = notificationEventRepository.findByIdAndUserId(notificationId, securityUtils.currentUserEntity().getId())
                .orElseThrow();
        event.setReadAt(OffsetDateTime.now());
        return NotificationResponse.from(event);
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse currentPreference() {
        return NotificationPreferenceResponse.from(preferenceFor(securityUtils.currentUserEntity()));
    }

    @Transactional
    public NotificationPreferenceResponse updatePreference(NotificationPreferenceRequest request) {
        NotificationPreference preference = preferenceFor(securityUtils.currentUserEntity());
        preference.setInAppEnabled(request.inAppEnabled());
        preference.setEmailEnabled(request.emailEnabled());
        return NotificationPreferenceResponse.from(preference);
    }

    @Transactional
    public void create(User user, String type, String title, String message, String actionUrl, com.bdcrm.lead.Lead lead, LeadFollowup followup) {
        NotificationPreference preference = preferenceFor(user);
        NotificationEvent event = new NotificationEvent();
        event.setUser(user);
        event.setOrganization(user.getOrganization());
        event.setType(type);
        event.setTitle(title);
        event.setMessage(message);
        event.setActionUrl(actionUrl);
        event.setLead(lead);
        event.setFollowup(followup);
        if (preference.isInAppEnabled()) {
            notificationEventRepository.save(event);
        }
        if (preference.isEmailEnabled()) {
            sendEmail(user, title, message);
            event.setEmailedAt(OffsetDateTime.now());
        }
    }

    @Transactional
    @Scheduled(cron = "0 30 * * * *")
    public void generateFollowupReminders() {
        OffsetDateTime todayStart = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDate today = LocalDate.now();
        for (LeadFollowup followup : followupRepository.findByStatusInOrderByDueDateAsc(List.of(FollowupStatus.DUE, FollowupStatus.OVERDUE))) {
            String type = null;
            String title = null;
            String message = null;
            if (followup.getStatus() == FollowupStatus.OVERDUE) {
                type = "FOLLOWUP_OVERDUE";
                title = "Overdue follow-up";
                message = "Follow-up " + followup.getStepNumber() + " for " + followup.getLead().getCompanyName() + " is overdue.";
            } else if (!followup.getDueDate().isAfter(today.plusDays(1))) {
                type = "FOLLOWUP_DUE_SOON";
                title = "Follow-up due soon";
                message = "Follow-up " + followup.getStepNumber() + " for " + followup.getLead().getCompanyName() + " is due soon.";
            }
            if (type != null && !notificationEventRepository.existsByUserIdAndTypeAndFollowupIdAndCreatedAtAfter(
                    followup.getAssignedUser().getId(), type, followup.getId(), todayStart)) {
                create(
                        followup.getAssignedUser(),
                        type,
                        title,
                        message,
                        "/followups",
                        followup.getLead(),
                        followup);
                if (followup.getAssignedUser().getManager() != null && followup.getStatus() == FollowupStatus.OVERDUE) {
                    create(
                            followup.getAssignedUser().getManager(),
                            "FOLLOWUP_MANAGER_ALERT",
                            "Manager escalation alert",
                            "An owned follow-up for " + followup.getLead().getCompanyName() + " is overdue.",
                            "/followups",
                            followup.getLead(),
                            followup);
                }
            }
        }
    }

    private NotificationPreference preferenceFor(User user) {
        return preferenceRepository.findByUserIdAndOrganizationId(user.getId(), user.getOrganization().getId())
                .orElseGet(() -> {
                    NotificationPreference preference = new NotificationPreference();
                    preference.setUser(user);
                    preference.setOrganization(user.getOrganization());
                    preference.setEmailEnabled(crmProperties.getNotifications().isEmailEnabled());
                    return preferenceRepository.save(preference);
                });
    }

    private void sendEmail(User user, String subject, String body) {
        if (!crmProperties.getNotifications().isEmailEnabled()) {
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(crmProperties.getNotifications().getFromAddress());
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
