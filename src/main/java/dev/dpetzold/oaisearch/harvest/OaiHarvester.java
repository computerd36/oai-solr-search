package dev.dpetzold.oaisearch.harvest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class OaiHarvester {

    private static final Logger log = LoggerFactory.getLogger(OaiHarvester.class);

    private final OaiProperties properties;
    private final OaiParser parser = new OaiParser();
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    OaiHarvester(OaiProperties properties) {
        this.properties = properties;
    }

    public List<OaiRecord> harvest() {
        List<OaiRecord> collected = new ArrayList<>();
        String token = null;
        int page = 0;

        do {
            if (page > 0) {
                pause();
            }
            OaiResponse response = parser.parse(get(token == null ? firstPageUri() : nextPageUri(token)));
            page++;
            collected.addAll(response.records());
            token = response.resumptionToken();
            log.debug("page {}: {} records, {} total", page, response.records().size(), collected.size());
        } while (token != null && collected.size() < properties.maxRecords());

        if (collected.size() > properties.maxRecords()) {
            collected = collected.subList(0, properties.maxRecords());
        }
        log.info("harvested {} records from {} in {} page(s)", collected.size(), properties.set(), page);
        return List.copyOf(collected);
    }

    private URI firstPageUri() {
        return URI.create(properties.baseUrl()
                + "?verb=ListRecords"
                + "&metadataPrefix=" + encode(properties.metadataPrefix())
                + "&set=" + encode(properties.set()));
    }

    private URI nextPageUri(String token) {
        // token arrives percent-encoded, encoding it again would turn % into %25
        return URI.create(properties.baseUrl() + "?verb=ListRecords&resumptionToken=" + token);
    }

    private byte[] get(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("User-Agent", properties.userAgent())
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("OAI request failed with HTTP " + response.statusCode() + ": " + uri);
            }
            return response.body();
        } catch (IOException e) {
            throw new IllegalStateException("OAI request failed: " + uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OAI request interrupted: " + uri, e);
        }
    }

    // public interface, no reason to hammer it
    private void pause() {
        try {
            Thread.sleep(properties.requestDelay());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("harvest interrupted", e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
