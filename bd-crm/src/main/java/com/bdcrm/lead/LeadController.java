package com.bdcrm.lead;

import com.bdcrm.common.PagedResponse;
import com.bdcrm.pipeline.LeadStageUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @PostMapping
    public LeadDetailResponse createLead(@Valid @RequestBody LeadCreateRequest request) {
        return leadService.createLead(request);
    }

    @PutMapping("/{leadId}")
    public LeadDetailResponse updateLead(@PathVariable Long leadId, @Valid @RequestBody LeadUpdateRequest request) {
        return leadService.updateLead(leadId, request);
    }

    @GetMapping
    public PagedResponse<LeadSummaryResponse> listLeads(
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) Long assignedUserId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return leadService.listLeads(status, assignedUserId, search, page, size);
    }

    @GetMapping("/{leadId}")
    public LeadDetailResponse getLead(@PathVariable Long leadId) {
        return leadService.getLead(leadId);
    }

    @PatchMapping("/{leadId}/assign")
    public LeadDetailResponse assignLead(@PathVariable Long leadId, @Valid @RequestBody LeadAssignmentRequest request) {
        return leadService.assignLead(leadId, request);
    }

    @PatchMapping("/{leadId}/status")
    public LeadDetailResponse updateStatus(@PathVariable Long leadId, @Valid @RequestBody LeadStatusUpdateRequest request) {
        return leadService.updateStatus(leadId, request);
    }

    @PatchMapping("/{leadId}/stage")
    public LeadDetailResponse updateStage(@PathVariable Long leadId, @Valid @RequestBody LeadStageUpdateRequest request) {
        return leadService.updateStage(leadId, request);
    }

    @PostMapping("/{leadId}/notes")
    public LeadNoteResponse addNote(@PathVariable Long leadId, @Valid @RequestBody LeadNoteRequest request) {
        return leadService.addNote(leadId, request);
    }

    @PostMapping("/bulk")
    public List<LeadSummaryResponse> bulkAction(@Valid @RequestBody BulkLeadActionRequest request) {
        return leadService.bulkAction(request);
    }
}
