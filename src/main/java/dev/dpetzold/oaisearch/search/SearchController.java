package dev.dpetzold.oaisearch.search;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Validated
class SearchController {

    private final SearchService service;

    SearchController(SearchService service) {
        this.service = service;
    }

    @GetMapping("/api/search")
    SearchResult search(
            @RequestParam(defaultValue = "") @Size(max = 200, message = "q must be at most 200 characters") String q,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") int page,
            // capped, otherwise someone asks for size=100000
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 100, message = "size must be at most 100") int size,
            @RequestParam(required = false) List<String> creator,
            @RequestParam(required = false) List<String> subject,
            @RequestParam(required = false) List<String> language,
            @RequestParam(required = false) List<String> year) {

        Map<String, List<String>> filters = new HashMap<>();
        filters.put("creator", creator);
        filters.put("subject", subject);
        filters.put("language", language);
        filters.put("year", year);

        return service.search(q, page, size, filters);
    }
}
