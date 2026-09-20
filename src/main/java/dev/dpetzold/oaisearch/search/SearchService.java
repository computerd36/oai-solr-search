package dev.dpetzold.oaisearch.search;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import dev.dpetzold.oaisearch.index.SolrProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    // facet name in the API -> solr field, used for both facet.field and fq
    static final Map<String, String> FACET_FIELDS = new LinkedHashMap<>(Map.of(
            "creator", "creator_ss",
            "subject", "subject_ss",
            "language", "language_ss",
            "year", "year_i"));

    // _txt_de instead of _txt: the corpus is German, text_general does not stem
    private static final String QUERY_FIELDS = "title_txt_de^2 creator_txt_de subject_txt_de";

    private final SolrClient solr;
    private final SolrProperties properties;

    SearchService(SolrClient solr, SolrProperties properties) {
        this.solr = solr;
        this.properties = properties;
    }

    public SearchResult search(String q, int page, int size, Map<String, List<String>> filters) {
        SolrQuery query = new SolrQuery(q == null ? "" : q.trim());
        query.set("defType", "edismax");
        query.set("qf", QUERY_FIELDS);
        // empty q falls back to q.alt, so a bare /api/search lists everything
        query.set("q.alt", "*:*");
        query.setStart(page * size);
        query.setRows(size);

        query.setFacet(true);
        query.setFacetMinCount(1);
        query.setFacetLimit(20);
        FACET_FIELDS.values().forEach(query::addFacetField);

        // filters stay in fq so they never influence scoring
        filters.forEach((name, values) -> {
            if (values != null && !values.isEmpty()) {
                query.addFilterQuery(FACET_FIELDS.get(name) + ":(" + orOf(values) + ")");
            }
        });

        QueryResponse response = execute(query);
        return new SearchResult(
                response.getResults().getNumFound(),
                page,
                size,
                response.getResults().stream().map(SearchService::toHit).toList(),
                facetsOf(response));
    }

    private QueryResponse execute(SolrQuery query) {
        try {
            return solr.query(properties.core(), query);
        } catch (SolrServerException | IOException e) {
            log.warn("solr query failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "search backend unavailable");
        }
    }

    private static Map<String, List<SearchResult.FacetValue>> facetsOf(QueryResponse response) {
        Map<String, List<SearchResult.FacetValue>> facets = new LinkedHashMap<>();
        FACET_FIELDS.forEach((name, field) -> {
            FacetField facet = response.getFacetField(field);
            if (facet == null) {
                return;
            }
            List<SearchResult.FacetValue> values = new ArrayList<>();
            facet.getValues().forEach(count ->
                    values.add(new SearchResult.FacetValue(count.getName(), count.getCount())));
            facets.put(name, List.copyOf(values));
        });
        return Map.copyOf(facets);
    }

    private static SearchResult.Hit toHit(SolrDocument document) {
        return new SearchResult.Hit(
                string(document, "id"),
                string(document, "title_txt_de"),
                strings(document, "creator_ss"),
                string(document, "date_s"),
                strings(document, "subject_ss"),
                strings(document, "language_ss"),
                strings(document, "type_ss"),
                string(document, "url_s"));
    }

    private static String string(SolrDocument document, String field) {
        Object value = document.getFirstValue(field);
        return value == null ? null : String.valueOf(value);
    }

    private static List<String> strings(SolrDocument document, String field) {
        Collection<Object> values = document.getFieldValues(field);
        return values == null ? List.of() : values.stream().map(String::valueOf).toList();
    }

    private static String orOf(List<String> values) {
        return String.join(" OR ", values.stream().map(SearchService::quote).toList());
    }

    private static String quote(String value) {
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
