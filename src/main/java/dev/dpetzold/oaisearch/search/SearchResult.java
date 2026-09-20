package dev.dpetzold.oaisearch.search;

import java.util.List;
import java.util.Map;

public record SearchResult(
        long total,
        int page,
        int size,
        List<Hit> items,
        Map<String, List<FacetValue>> facets) {

    public record Hit(
            String ppn,
            String title,
            List<String> creators,
            String date,
            List<String> subjects,
            List<String> languages,
            List<String> types,
            String url) {
    }

    public record FacetValue(String value, long count) {
    }
}
