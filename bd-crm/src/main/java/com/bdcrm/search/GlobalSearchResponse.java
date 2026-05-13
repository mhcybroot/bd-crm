package com.bdcrm.search;

import java.util.List;

public record GlobalSearchResponse(
        List<SearchItem> leads,
        List<SearchItem> notes,
        List<SearchItem> activities,
        List<SearchItem> followups,
        List<SearchItem> attachments) {

    public record SearchItem(
            String type,
            Long id,
            Long leadId,
            String title,
            String subtitle) {
    }
}
