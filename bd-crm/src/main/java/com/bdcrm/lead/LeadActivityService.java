package com.bdcrm.lead;

import com.bdcrm.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LeadActivityService {

    private final LeadActivityRepository leadActivityRepository;

    public void log(Lead lead, User actor, LeadActivityType type, String description) {
        LeadActivity activity = new LeadActivity();
        activity.setLead(lead);
        activity.setActor(actor);
        activity.setType(type);
        activity.setDescription(description);
        leadActivityRepository.save(activity);
    }
}
