package dev.dpetzold.oaisearch.search;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import dev.dpetzold.oaisearch.index.SolrProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchServiceTest {

    private final SolrClient solr = mock(SolrClient.class);
    private final SearchService service = new SearchService(solr, new SolrProperties("http://localhost:8983/solr", "sbb"));

    @BeforeEach
    void emptyResponse() throws Exception {
        QueryResponse response = new QueryResponse();
        NamedList<Object> body = new NamedList<>();
        body.add("response", new SolrDocumentList());
        response.setResponse(body);
        when(solr.query(eq("sbb"), any(SolrQuery.class))).thenReturn(response);
    }

    @Test
    void putsFiltersIntoFqAndNotIntoTheMainQuery() {
        service.search("Teufel", 0, 20, Map.of("language", List.of("ger")));

        SolrQuery query = captureQuery();
        assertThat(query.getQuery()).isEqualTo("Teufel");
        assertThat(query.getFilterQueries()).containsExactly("language_ss:(\"ger\")");
    }

    @Test
    void combinesSeveralValuesOfOneFacetWithOr() {
        service.search("", 0, 20, Map.of("language", List.of("fre", "lat")));

        assertThat(captureQuery().getFilterQueries()).containsExactly("language_ss:(\"fre\" OR \"lat\")");
    }

    @Test
    void ignoresEmptyFilters() {
        service.search("", 0, 20, Map.of("language", List.of(), "creator", List.of("Jean Paul")));

        assertThat(captureQuery().getFilterQueries()).containsExactly("creator_ss:(\"Jean Paul\")");
    }

    @Test
    void asksForEveryFacetAndTranslatesPagingToStartAndRows() {
        service.search("", 3, 10, Map.of());

        SolrQuery query = captureQuery();
        assertThat(query.getFacetFields()).containsExactly("creator_ss", "subject_ss", "language_ss", "year_i");
        assertThat(query.getStart()).isEqualTo(30);
        assertThat(query.getRows()).isEqualTo(10);
    }

    @Test
    void escapesQuotesSoAValueCannotBreakOutOfTheFilter() {
        service.search("", 0, 20, Map.of("creator", List.of("a\" OR b:*")));

        assertThat(captureQuery().getFilterQueries()).containsExactly("creator_ss:(\"a\\\" OR b:*\")");
    }

    @Test
    void turnsAnUnreachableSolrIntoServiceUnavailable() throws Exception {
        when(solr.query(eq("sbb"), any(SolrQuery.class))).thenThrow(new IOException("connection refused"));

        assertThatThrownBy(() -> service.search("", 0, 20, Map.of()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503")
                .hasMessageContaining("search backend unavailable");
    }

    private SolrQuery captureQuery() {
        ArgumentCaptor<SolrQuery> captor = ArgumentCaptor.forClass(SolrQuery.class);
        try {
            verify(solr).query(eq("sbb"), captor.capture());
        } catch (SolrServerException | IOException e) {
            throw new IllegalStateException(e);
        }
        return captor.getValue();
    }
}
