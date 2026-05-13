package com.bdcrm.export;

import com.bdcrm.lead.LeadRepository;
import com.bdcrm.reporting.ReportFilterRequest;
import com.bdcrm.reporting.ReportingService;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final LeadRepository leadRepository;
    private final ReportingService reportingService;

    @Transactional(readOnly = true)
    public String leadsCsv() {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("id,companyName,contactName,status,priority,owner,template,stage");
        leadRepository.findAll().forEach(lead -> joiner.add(String.join(",",
                String.valueOf(lead.getId()),
                csv(lead.getCompanyName()),
                csv(lead.getContactName()),
                lead.getStatus().name(),
                lead.getPriority().name(),
                csv(lead.getAssignedUser().getFullName()),
                csv(lead.getTemplate().getName()),
                csv(lead.getCurrentStage() != null ? lead.getCurrentStage().getName() : ""))));
        return joiner.toString();
    }

    @Transactional(readOnly = true)
    public String reportCsv(ReportFilterRequest filter) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("rep,assignedLeads,pendingFollowups,completedFollowups");
        reportingService.overview(filter).reps().forEach(rep -> joiner.add(String.join(",",
                csv(rep.userName()),
                String.valueOf(rep.assignedLeads()),
                String.valueOf(rep.pendingFollowups()),
                String.valueOf(rep.completedFollowups()))));
        return joiner.toString();
    }

    private String csv(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }
}
