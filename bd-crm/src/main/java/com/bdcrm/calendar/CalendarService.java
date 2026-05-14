package com.bdcrm.calendar;

import com.bdcrm.followup.LeadFollowup;
import com.bdcrm.followup.LeadFollowupRepository;
import com.bdcrm.lead.Lead;
import com.bdcrm.user.User;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final LeadFollowupRepository leadFollowupRepository;

    private static final DateTimeFormatter ICAL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    @Transactional(readOnly = true)
    public String exportFollowupsToIcal(LocalDate from, LocalDate to, User user, boolean managerView) {
        var followups = leadFollowupRepository.findAll().stream()
                .filter(f -> f.getStatus() != com.bdcrm.followup.FollowupStatus.COMPLETED
                        && f.getStatus() != com.bdcrm.followup.FollowupStatus.CANCELLED
                        && f.getStatus() != com.bdcrm.followup.FollowupStatus.SKIPPED)
                .filter(f -> !f.getDueDate().isBefore(from) && !f.getDueDate().isAfter(to))
                .filter(f -> managerView || f.getAssignedUser().getId().equals(user.getId()))
                .toList();

        StringBuilder ical = new StringBuilder();
        ical.append("BEGIN:VCALENDAR\r\n");
        ical.append("VERSION:2.0\r\n");
        ical.append("PRODID:-//BD-CRM//Follow-up Calendar//EN\r\n");
        ical.append("CALSCALE:GREGORIAN\r\n");
        ical.append("METHOD:PUBLISH\r\n");
        ical.append("X-WR-CALNAME:BD-CRM Follow-ups\r\n");

        for (LeadFollowup f : followups) {
            ical.append(icalEvent(f));
        }

        ical.append("END:VCALENDAR\r\n");
        return ical.toString();
    }

    private String icalEvent(LeadFollowup followup) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VEVENT\r\n");

        String uid = "followup-" + followup.getId() + "@bd-crm";
        sb.append("UID:").append(uid).append("\r\n");

        var dueInstant = followup.getDueDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        var dtStart = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(java.time.ZoneOffset.UTC)
                .format(dueInstant);
        sb.append("DTSTART:").append(dtStart).append("\r\n");
        sb.append("DTEND:").append(dtStart).append("\r\n");

        String summary = "Follow-up #" + followup.getStepNumber() + " - " + followup.getLead().getCompanyName();
        sb.append("SUMMARY:").append(escapeIcalText(summary)).append("\r\n");

        String description = "Lead: " + followup.getLead().getContactName() + " (" + followup.getLead().getCompanyName() + ")\n";
        description += "Channel: " + followup.getChannel() + "\n";
        if (followup.getInstructions() != null) {
            description += "Instructions: " + followup.getInstructions();
        }
        sb.append("DESCRIPTION:").append(escapeIcalText(description)).append("\r\n");

        sb.append("CATEGORIES:FOLLOWUP\r\n");

        var createdInstant = followup.getCreatedAt() != null ? followup.getCreatedAt().toInstant() : java.time.Instant.now();
        var createdStr = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(java.time.ZoneOffset.UTC)
                .format(createdInstant);
        sb.append("DTSTAMP:").append(createdStr).append("\r\n");

        sb.append("END:VEVENT\r\n");
        return sb.toString();
    }

    private String escapeIcalText(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace(",", "\\,")
                .replace(";", "\\;");
    }
}