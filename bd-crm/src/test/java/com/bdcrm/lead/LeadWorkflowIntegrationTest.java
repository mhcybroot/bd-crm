package com.bdcrm.lead;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdcrm.followup.FollowupService;
import com.bdcrm.followup.FollowupStatus;
import com.bdcrm.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
class LeadWorkflowIntegrationTest {

    @Autowired
    private LeadService leadService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createLeadGeneratesTemplateFollowups() {
        authenticateAs("rep");

        LeadDetailResponse lead = leadService.createLead(new LeadCreateRequest(
                "Acme Corp",
                "Jane Doe",
                "jane@acme.test",
                "12345",
                "LinkedIn",
                "Warm lead",
                LeadPriority.HIGH,
                null,
                null));

        assertThat(lead.followups()).hasSize(7);
        assertThat(lead.followups().get(0).stepNumber()).isEqualTo(1);
        assertThat(lead.followups()).allMatch(followup -> followup.status() == FollowupStatus.DUE);
    }

    @Test
    void winningLeadClosesOpenFollowups() {
        authenticateAs("rep");

        LeadDetailResponse lead = leadService.createLead(new LeadCreateRequest(
                "Beta Ltd",
                "John Smith",
                "john@beta.test",
                "55555",
                "Referral",
                "Hot lead",
                LeadPriority.HIGH,
                null,
                null));

        LeadDetailResponse updated = leadService.updateStatus(lead.lead().id(), new LeadStatusUpdateRequest(LeadStatus.WON));

        assertThat(updated.lead().status()).isEqualTo(LeadStatus.WON);
        assertThat(updated.followups()).allMatch(followup -> followup.status() == FollowupStatus.CANCELLED);
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
