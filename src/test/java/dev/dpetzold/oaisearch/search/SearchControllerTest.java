package dev.dpetzold.oaisearch.search;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SearchService service;

    @Test
    void returnsTotalItemsAndFacets() throws Exception {
        when(service.search(anyString(), anyInt(), anyInt(), any())).thenReturn(result());

        mvc.perform(get("/api/search").param("q", "Teufel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(24))
                .andExpect(jsonPath("$.items[0].ppn").value("PPN1"))
                .andExpect(jsonPath("$.items[0].title").value("Ein Titel"))
                .andExpect(jsonPath("$.facets.language[0].value").value("ger"));
    }

    @Test
    void passesPagingThrough() throws Exception {
        when(service.search(anyString(), anyInt(), anyInt(), any())).thenReturn(result());

        mvc.perform(get("/api/search").param("page", "2").param("size", "5"))
                .andExpect(status().isOk());

        verify(service).search(anyString(), eq(2), eq(5), any());
    }

    @Test
    void defaultsToFirstPageAndTwentyHits() throws Exception {
        when(service.search(anyString(), anyInt(), anyInt(), any())).thenReturn(result());

        mvc.perform(get("/api/search")).andExpect(status().isOk());

        verify(service).search(anyString(), eq(0), eq(20), any());
    }

    @Test
    void rejectsASizeAboveTheCap() throws Exception {
        mvc.perform(get("/api/search").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("size must be at most 100"));
    }

    @Test
    void rejectsANegativePage() throws Exception {
        mvc.perform(get("/api/search").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("page must be 0 or greater"));
    }

    private static SearchResult result() {
        return new SearchResult(
                24, 0, 20,
                List.of(new SearchResult.Hit("PPN1", "Ein Titel", List.of("Jean Paul"), "1783",
                        List.of(), List.of("ger"), List.of("manuscript"), "https://example.org/1")),
                Map.of("language", List.of(new SearchResult.FacetValue("ger", 613))));
    }
}
