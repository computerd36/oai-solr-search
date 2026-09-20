package dev.dpetzold.oaisearch.index;

import org.apache.solr.common.SolrInputDocument;
import dev.dpetzold.oaisearch.harvest.OaiRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SolrIndexerTest {

    @Test
    void mapsRecordOntoDynamicFields() {
        SolrInputDocument document = SolrIndexer.toDocument(record("1783"));

        assertThat(document.getFieldValue("id")).isEqualTo("PPN1");
        assertThat(document.getFieldValue("title_txt_de")).isEqualTo("Ein Titel");
        assertThat(document.getFieldValues("creator_ss")).containsExactly("Jean Paul");
        assertThat(document.getFieldValues("type_ss")).containsExactly("manuscript", "text");
        assertThat(document.getFieldValue("date_s")).isEqualTo("1783");
        assertThat(document.getFieldValue("year_i")).isEqualTo(1783);
    }

    @Test
    void joinsRepeatedValuesIntoTheSearchableField() {
        SolrInputDocument document = SolrIndexer.toDocument(record("1783"));

        assertThat(document.getFieldValue("subject_txt_de")).isEqualTo("Jean Paul Nachlässe");
    }

    @Test
    void leavesOutTheYearWhenTheDateIsNotNumeric() {
        SolrInputDocument document = SolrIndexer.toDocument(record("100X"));

        assertThat(document.getFieldValue("year_i")).isNull();
        assertThat(document.getFieldValue("date_s")).isEqualTo("100X");
    }

    private static OaiRecord record(String date) {
        return new OaiRecord(
                "PPN1",
                "Ein Titel",
                List.of("Jean Paul"),
                date,
                List.of("Jean Paul", "Nachlässe"),
                List.of("ger"),
                List.of("manuscript", "text"),
                "https://resolver.staatsbibliothek-berlin.de/SBB1");
    }
}
