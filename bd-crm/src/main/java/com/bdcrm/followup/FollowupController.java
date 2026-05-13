package com.bdcrm.followup;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/followups")
@RequiredArgsConstructor
public class FollowupController {

    private final FollowupService followupService;

    @GetMapping
    public List<LeadFollowupResponse> listQueue(@RequestParam(defaultValue = "open") String status) {
        return followupService.listWorkQueue(status);
    }

    @PatchMapping("/{followupId}/complete")
    public LeadFollowupResponse complete(@PathVariable Long followupId, @Valid @RequestBody FollowupActionRequest request) {
        return followupService.complete(followupId, request);
    }

    @PatchMapping("/{followupId}/reschedule")
    public LeadFollowupResponse reschedule(@PathVariable Long followupId, @Valid @RequestBody FollowupActionRequest request) {
        return followupService.reschedule(followupId, request);
    }

    @PatchMapping("/{followupId}/skip")
    public LeadFollowupResponse skip(@PathVariable Long followupId, @Valid @RequestBody FollowupActionRequest request) {
        return followupService.skip(followupId, request);
    }

    @PatchMapping("/{followupId}/reassign")
    public LeadFollowupResponse reassign(@PathVariable Long followupId, @Valid @RequestBody FollowupActionRequest request) {
        return followupService.reassign(followupId, request);
    }
}
