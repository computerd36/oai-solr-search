package dev.dpetzold.oaisearch.index;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import dev.dpetzold.oaisearch.harvest.OaiRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class SolrIndexer {

    private static final Logger log = LoggerFactory.getLogger(SolrIndexer.class);

    private static final int BATCH_SIZE = 500;
    private static final Pattern YEAR = Pattern.compile("\\d{1,4}");

    private final SolrClient solr;
    private final SolrProperties properties;

    SolrIndexer(SolrClient solr, SolrProperties properties) {
        this.solr = solr;
        this.properties = properties;
    }

    public int index(List<OaiRecord> records) {
        try {
            for (int from = 0; from < records.size(); from += BATCH_SIZE) {
                List<OaiRecord> batch = records.subList(from, Math.min(from + BATCH_SIZE, records.size()));
                solr.add(properties.core(), batch.stream().map(SolrIndexer::toDocument).toList());
                log.debug("sent {} documents, {} of {}", batch.size(), from + batch.size(), records.size());
            }
            solr.commit(properties.core());
        } catch (SolrServerException | IOException e) {
            throw new IllegalStateException("indexing into core " + properties.core() + " failed", e);
        }
        log.info("indexed {} documents into core {}", records.size(), properties.core());
        return records.size();
    }

    static SolrInputDocument toDocument(OaiRecord record) {
        SolrInputDocument document = new SolrInputDocument();

        // PPN is the uniqueKey, so a second run overwrites instead of duplicating
        document.addField("id", record.ppn());

        document.addField("title_txt", record.title());
        document.addField("date_s", record.date());
        document.addField("url_s", record.url());

        document.addField("creator_ss", record.creators());
        document.addField("subject_ss", record.subjects());
        document.addField("language_ss", record.languages());
        document.addField("type_ss", record.types());

        // _ss is string type, exact match only. Solr 9 ships no copyFields,
        // so the same values go into a _txt field for search.
        document.addField("creator_txt", String.join(" ", record.creators()));
        document.addField("subject_txt", String.join(" ", record.subjects()));

        Integer year = yearOf(record.date());
        if (year != null) {
            document.addField("year_i", year);
        }
        return document;
    }

    // dc:date also holds things like "100X" for an uncertain decade
    private static Integer yearOf(String date) {
        return date != null && YEAR.matcher(date).matches() ? Integer.valueOf(date) : null;
    }
}
