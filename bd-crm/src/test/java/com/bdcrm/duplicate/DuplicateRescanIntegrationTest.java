package com.bdcrm.duplicate;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdcrm.lead.LeadCreateRequest;
import com.bdcrm.lead.LeadDetailResponse;
import com.bdcrm.lead.LeadPriority;
import com.bdcrm.lead.LeadRepository;
import com.bdcrm.lead.LeadService;
import com.bdcrm.lead.LeadUpdateRequest;
import com.bdcrm.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
class DuplicateRescanIntegrationTest {

    @Autowired
    private DuplicateService duplicateService;

    @Autowired
    private DuplicateMatchRepository duplicateMatchRepository;

    @Autowired
    private LeadService leadService;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void rescanKeepsSingleCanonicalMatchAndResetsReviewedPairs() {
        authenticateAs("manager");
        LeadDetailResponse first = createLead("Atlas Labs", "Nadia Rahman", "shared@atlas.test", "01710000001");
        LeadDetailResponse second = createLead("Atlas Lab", "Imran Shah", "shared@atlas.test", "01710000001");

        duplicateService.rescan();
        DuplicateMatch created = duplicateMatchRepository.findAll().getFirst();
        duplicateService.updateState(created.getId(), DuplicateState.REVIEWED);

        duplicateService.rescan();

        assertThat(duplicateMatchRepository.count()).isEqualTo(1);
        DuplicateMatch rescanned = duplicateMatchRepository.findAll().getFirst();
        assertThat(rescanned.getLead().getId()).isLessThan(rescanned.getMatchedLead().getId());
        assertThat(rescanned.getState()).isEqualTo(DuplicateState.SUSPECTED);
        assertThat(rescanned.getReviewedBy()).isNull();
        assertThat(rescanned.getReviewedAt()).isNull();
        assertThat(leadRepository.findById(first.lead().id()).orElseThrow().getDuplicateState()).isEqualTo(DuplicateState.SUSPECTED);
        assertThat(leadRepository.findById(second.lead().id()).orElseThrow().getDuplicateState()).isEqualTo(DuplicateState.SUSPECTED);
    }

    @Test
    void rescanRemovesStaleMatchesAndClearsLeadStates() {
        authenticateAs("manager");
        LeadDetailResponse first = createLead("Beacon Works", "Jui Ahmed", "shared@beacon.test", "01710000002");
        LeadDetailResponse second = createLead("Beacon Work", "Farhan Ali", "shared@beacon.test", "01710000002");

        duplicateService.rescan();
        assertThat(duplicateMatchRepository.count()).isEqualTo(1);

        leadService.updateLead(second.lead().id(), new LeadUpdateRequest(
                "Harbor Retail",
                second.lead().contactName(),
                "harbor@retail.test",
                "01990000009",
                second.lead().source(),
                "No longer a duplicate",
                second.lead().priority(),
                second.lead().assignedUserId(),
                second.lead().templateId()));

        duplicateService.rescan();

        assertThat(duplicateMatchRepository.count()).isZero();
        assertThat(leadRepository.findById(first.lead().id()).orElseThrow().getDuplicateState()).isEqualTo(DuplicateState.CLEAR);
        assertThat(leadRepository.findById(second.lead().id()).orElseThrow().getDuplicateState()).isEqualTo(DuplicateState.CLEAR);
    }

    private LeadDetailResponse createLead(String companyName, String contactName, String email, String phone) {
        return leadService.createLead(new LeadCreateRequest(
                companyName,
                contactName,
                email,
                phone,
                "Referral",
                "Duplicate test lead",
                LeadPriority.MEDIUM,
                null,
                null));
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
