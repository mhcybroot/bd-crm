package com.bdcrm.followup;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdcrm.lead.LeadCreateRequest;
import com.bdcrm.lead.LeadDetailResponse;
import com.bdcrm.lead.LeadPriority;
import com.bdcrm.lead.LeadService;
import com.bdcrm.user.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(properties = "crm.escalation.threshold-days=0")
class FollowupEscalationIntegrationTest {

    @Autowired
    private LeadService leadService;

    @Autowired
    private LeadFollowupRepository leadFollowupRepository;

    @Autowired
    private EscalationEventRepository escalationEventRepository;

    @Autowired
    private FollowupService followupService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void overdueFollowupCreatesEscalationEvent() {
        authenticateAs("rep");
        LeadDetailResponse lead = leadService.createLead(new LeadCreateRequest(
                "Gamma Inc",
                "Sam Lee",
                "sam@gamma.test",
                "77777",
                "Web",
                "Needs follow-up",
                LeadPriority.MEDIUM,
                null,
                null));

        LeadFollowup first = leadFollowupRepository.findByLeadIdOrderByStepNumberAsc(lead.lead().id()).getFirst();
        first.setDueDate(LocalDate.now().minusDays(1));
        leadFollowupRepository.save(first);

        followupService.refreshOverdueAndEscalations();

        LeadFollowup updated = leadFollowupRepository.findById(first.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(FollowupStatus.OVERDUE);
        assertThat(escalationEventRepository.existsByFollowupId(first.getId())).isTrue();
    }

    private void authenticateAs(String username) {
        var user = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        com.bdcrm.auth.AuthenticatedUser.from(user),
                        user.getPassword(),
                        com.bdcrm.auth.AuthenticatedUser.from(user).getAuthorities()));
    }
}
