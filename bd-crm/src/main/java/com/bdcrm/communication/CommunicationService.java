package com.bdcrm.communication;

import com.bdcrm.auth.SecurityUtils;
import com.bdcrm.lead.Lead;
import com.bdcrm.lead.LeadActivityService;
import com.bdcrm.lead.LeadActivityType;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunicationService {

    private final LeadCommunicationRepository communicationRepository;
    private final SecurityUtils securityUtils;
    private final LeadActivityService leadActivityService;

    @Transactional(readOnly = true)
    public List<LeadCommunicationResponse> listForLead(Long leadId) {
        return communicationRepository.findByLeadIdOrderByOccurredAtDesc(leadId).stream()
                .map(LeadCommunicationResponse::from)
                .toList();
    }

    @Transactional
    public LeadCommunicationResponse log(Lead lead, LeadCommunicationRequest request) {
        LeadCommunication communication = new LeadCommunication();
        communication.setLead(lead);
        communication.setActor(securityUtils.currentUserEntity());
        communication.setChannel(request.channel());
        communication.setSubject(request.subject());
        communication.setBody(request.body());
        communication.setOutcome(request.outcome());
        communication.setOccurredAt(request.occurredAt() != null ? request.occurredAt() : OffsetDateTime.now());
        communication = communicationRepository.save(communication);
        leadActivityService.log(lead, communication.getActor(), LeadActivityType.COMMUNICATION_LOGGED,
                "Logged " + request.channel() + " touchpoint");
        return LeadCommunicationResponse.from(communication);
    }
}
