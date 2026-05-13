package com.bdcrm.qualification;

import com.bdcrm.lead.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/qualifications")
@RequiredArgsConstructor
public class QualificationController {

    private final QualificationService qualificationService;
    private final LeadService leadService;

    @GetMapping("/leads/{leadId}")
    public LeadQualificationResponse get(@PathVariable Long leadId) {
        return qualificationService.getResponse(leadService.requireVisibleLeadEntity(leadId));
    }

    @PutMapping("/leads/{leadId}")
    public LeadQualificationResponse update(@PathVariable Long leadId, @Valid @RequestBody LeadQualificationRequest request) {
        return qualificationService.update(leadService.requireVisibleLeadEntity(leadId), request);
    }
}
