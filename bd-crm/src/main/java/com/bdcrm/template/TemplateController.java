package com.bdcrm.template;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/followup-templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public List<FollowupTemplateResponse> listTemplates() {
        return templateService.listTemplates();
    }

    @GetMapping("/{templateId}")
    public FollowupTemplateResponse getTemplate(@PathVariable Long templateId) {
        return FollowupTemplateResponse.from(templateService.getTemplateEntity(templateId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public FollowupTemplateResponse createTemplate(@Valid @RequestBody FollowupTemplateRequest request) {
        return templateService.createTemplate(request);
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
    public FollowupTemplateResponse updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody FollowupTemplateRequest request) {
        return templateService.updateTemplate(templateId, request);
    }
}
