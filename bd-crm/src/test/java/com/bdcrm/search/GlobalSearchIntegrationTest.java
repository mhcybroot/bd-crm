package com.bdcrm.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdcrm.followup.FollowupActionRequest;
import com.bdcrm.followup.FollowupOutcome;
import com.bdcrm.followup.FollowupService;
import com.bdcrm.lead.LeadCreateRequest;
import com.bdcrm.lead.LeadDetailResponse;
import com.bdcrm.lead.LeadNoteRequest;
import com.bdcrm.lead.LeadPriority;
import com.bdcrm.lead.LeadRepository;
import com.bdcrm.lead.LeadService;
import com.bdcrm.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
class GlobalSearchIntegrationTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private LeadService leadService;

    @Autowired
    private FollowupService followupService;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void searchMatchesLeadFieldsAndRelatedNotesAndFollowups() {
        authenticateAs("rep");
        LeadDetailResponse primary = leadService.createLead(new LeadCreateRequest(
                "Searchable Systems",
                "Nadia Khan",
                "nadia@searchable.test",
                "8801700000000",
                "Referral Network",
                "Primary searchable lead",
                LeadPriority.HIGH,
                null,
                null));
        leadService.addNote(primary.lead().id(), new LeadNoteRequest("Meeting recap prepared by BD Rep for the proposal review."));
        var firstFollowup = followupService.findForLead(primary.lead().id()).getFirst();
        followupService.complete(firstFollowup.id(), new FollowupActionRequest(
                null,
                FollowupOutcome.INTERESTED,
                null,
                "Customer asked for proposal after the pricing deck follow-up."));

        LeadDetailResponse merged = leadService.createLead(new LeadCreateRequest(
                "Merged Searchable",
                "Archived Contact",
                "archived@searchable.test",
                "8801800000000",
                "Web",
                "Merged lead should not appear",
                LeadPriority.LOW,
                null,
                null));
        var mergedLead = leadRepository.findById(merged.lead().id()).orElseThrow();
        mergedLead.setMergedIntoLeadId(primary.lead().id());
        leadRepository.save(mergedLead);

        GlobalSearchResponse emailSearch = searchService.search("searchable.test", null, null, null, null, null, null, null);
        assertThat(emailSearch.leads()).extracting(GlobalSearchResponse.SearchItem::leadId).contains(primary.lead().id());

        GlobalSearchResponse phoneSearch = searchService.search("8801700000000", null, null, null, null, null, null, null);
        assertThat(phoneSearch.leads()).extracting(GlobalSearchResponse.SearchItem::leadId).contains(primary.lead().id());

        GlobalSearchResponse sourceSearch = searchService.search("referral network", null, null, null, null, null, null, null);
        assertThat(sourceSearch.leads()).extracting(GlobalSearchResponse.SearchItem::leadId).contains(primary.lead().id());

        GlobalSearchResponse noteSearch = searchService.search("bd rep", null, null, null, null, null, null, null);
        assertThat(noteSearch.notes()).singleElement().satisfies(item -> {
            assertThat(item.leadId()).isEqualTo(primary.lead().id());
            assertThat(item.title()).contains("Searchable Systems");
            assertThat(item.subtitle()).contains("Meeting recap");
        });

        GlobalSearchResponse followupSearch = searchService.search("proposal", null, null, null, null, null, null, null);
        assertThat(followupSearch.followups()).singleElement().satisfies(item -> {
            assertThat(item.leadId()).isEqualTo(primary.lead().id());
            assertThat(item.title()).contains("Searchable Systems");
            assertThat(item.subtitle()).contains("COMPLETED");
            assertThat(item.subtitle()).contains("INTERESTED");
        });

        GlobalSearchResponse parentContextSearch = searchService.search("searchable systems", null, null, null, null, null, null, null);
        assertThat(parentContextSearch.notes()).extracting(GlobalSearchResponse.SearchItem::leadId).contains(primary.lead().id());
        assertThat(parentContextSearch.followups()).extracting(GlobalSearchResponse.SearchItem::leadId).contains(primary.lead().id());

        GlobalSearchResponse mergedLeadSearch = searchService.search("merged searchable", null, null, null, null, null, null, null);
        assertThat(mergedLeadSearch.leads()).isEmpty();
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
