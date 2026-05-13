package com.bdcrm.savedview;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/saved-views")
@RequiredArgsConstructor
public class SavedViewController {

    private final SavedViewService savedViewService;

    @GetMapping
    public List<SavedViewResponse> list(@RequestParam String pageKey) {
        return savedViewService.list(pageKey);
    }

    @PostMapping
    public SavedViewResponse save(@Valid @RequestBody SavedViewRequest request) {
        return savedViewService.save(request);
    }
}
