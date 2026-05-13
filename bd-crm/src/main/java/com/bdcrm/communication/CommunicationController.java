package com.bdcrm.communication;

import com.bdcrm.lead.LeadService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leads/{leadId}/communications")
@RequiredArgsConstructor
public class CommunicationController {

    private final CommunicationService communicationService;
    private final LeadService leadService;

    @GetMapping
    public List<LeadCommunicationResponse> list(@PathVariable Long leadId) {
        leadService.requireVisibleLeadEntity(leadId);
        return communicationService.listForLead(leadId);
    }

    @PostMapping
    public LeadCommunicationResponse log(@PathVariable Long leadId, @Valid @RequestBody LeadCommunicationRequest request) {
        return communicationService.log(leadService.requireVisibleLeadEntity(leadId), request);
    }
}
