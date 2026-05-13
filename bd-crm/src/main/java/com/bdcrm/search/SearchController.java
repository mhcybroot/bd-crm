package com.bdcrm.search;

import com.bdcrm.lead.LeadStatus;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public GlobalSearchResponse search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long owner,
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) Long stage,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return searchService.search(q, owner, status, stage, source, outcome, dateFrom, dateTo);
    }
}
