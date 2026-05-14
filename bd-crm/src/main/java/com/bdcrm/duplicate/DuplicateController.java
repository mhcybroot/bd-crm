package com.bdcrm.duplicate;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/duplicates")
@RequiredArgsConstructor
public class DuplicateController {

    private final DuplicateService duplicateService;

    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ORG_MANAGER')")
    public List<DuplicateCandidateResponse> scan() {
        return duplicateService.rescan();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ORG_MANAGER')")
    public List<DuplicateCandidateResponse> list() {
        return duplicateService.list();
    }

    @PatchMapping("/{duplicateId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ORG_MANAGER')")
    public DuplicateCandidateResponse update(@PathVariable Long duplicateId, @RequestParam DuplicateState state) {
        return duplicateService.updateState(duplicateId, state);
    }

    @PostMapping("/merge")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ORG_MANAGER')")
    public void merge(@Valid @RequestBody LeadMergeRequest request) {
        duplicateService.merge(request);
    }
}
