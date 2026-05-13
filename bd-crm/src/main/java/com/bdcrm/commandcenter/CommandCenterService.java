package com.bdcrm.commandcenter;

import com.bdcrm.duplicate.DuplicateService;
import com.bdcrm.followup.FollowupService;
import com.bdcrm.lead.LeadRepository;
import com.bdcrm.notification.NotificationService;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommandCenterService {

    private final NotificationService notificationService;
    private final FollowupService followupService;
    private final DuplicateService duplicateService;
    private final LeadRepository leadRepository;

    @Transactional(readOnly = true)
    public CommandCenterResponse current() {
        var due = followupService.listWorkQueue("due");
        var overdue = followupService.listWorkQueue("overdue");
        var duplicates = duplicateService.list().stream().limit(5).toList();
        var recommendations = new ArrayList<String>();
        if (!overdue.isEmpty()) {
            recommendations.add("Prioritize overdue follow-ups before opening new outreach.");
        }
        if (!duplicates.isEmpty()) {
            recommendations.add("Review suspected duplicates to protect reporting accuracy.");
        }
        long unqualifiedLeads = leadRepository.findAll().stream().filter(lead -> lead.getMergedIntoLeadId() == null).count();
        if (unqualifiedLeads > 0) {
            recommendations.add("Refresh qualification scores on active leads to improve stage decisions.");
        }
        return new CommandCenterResponse(
                notificationService.listForCurrentUser(),
                due,
                overdue,
                duplicates,
                recommendations);
    }
}
