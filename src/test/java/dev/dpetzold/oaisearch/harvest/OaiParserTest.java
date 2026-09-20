package dev.dpetzold.oaisearch.harvest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OaiParserTest {

    private final OaiParser parser = new OaiParser();

    @Test
    void readsAllRecordsOfARealPage() {
        OaiResponse response = parser.parse(fixture("list-records-page1.xml"));

        assertThat(response.records()).hasSize(50);
        assertThat(response.hasMore()).isTrue();
    }

    @Test
    void mapsDublinCoreOntoTheRecord() {
        OaiRecord first = parser.parse(fixture("list-records-page1.xml")).records().getFirst();

        assertThat(first.ppn()).isEqualTo("PPN1877460214");
        assertThat(first.title()).startsWith("Auswahl aus des Teufels Papieren");
        assertThat(first.creators()).containsExactly("Jean Paul");
        assertThat(first.date()).isEqualTo("1783");
        assertThat(first.languages()).containsExactly("ger");
        assertThat(first.url()).isEqualTo("https://resolver.staatsbibliothek-berlin.de/SBB00035AB500000000");
    }

    @Test
    void keepsRepeatedElementsAsLists() {
        OaiRecord first = parser.parse(fixture("list-records-page1.xml")).records().getFirst();

        assertThat(first.types()).containsExactly("manuscript", "text");
        assertThat(first.subjects()).containsExactly("Jean Paul", "Nachlässe und Autographe");
    }

    @Test
    void picksTheResolverUrlAndNotThePpnOutOfDcIdentifier() {
        List<OaiRecord> records = parser.parse(fixture("list-records-page1.xml")).records();

        assertThat(records).allSatisfy(record -> assertThat(record.url()).startsWith("https://"));
    }

    @Test
    void skipsDeletedRecords() {
        OaiResponse response = parser.parse(fixture("list-records-deleted.xml"));

        assertThat(response.records())
                .extracting(OaiRecord::ppn)
                .containsExactly("PPN000000002");
    }

    @Test
    void reportsNoTokenOnTheLastPage() {
        OaiResponse response = parser.parse(fixture("list-records-last-page.xml"));

        assertThat(response.resumptionToken()).isNull();
        assertThat(response.hasMore()).isFalse();
    }

    @Test
    void failsOnAnOaiError() {
        byte[] xml = fixture("error-response.xml");

        assertThatThrownBy(() -> parser.parse(xml))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("badArgument");
    }

    private static byte[] fixture(String name) {
        try (InputStream in = OaiParserTest.class.getResourceAsStream("/oai/" + name)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(name, e);
        }
    }
}
