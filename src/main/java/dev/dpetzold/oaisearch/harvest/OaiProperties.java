package dev.dpetzold.oaisearch.harvest;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "oai")
public record OaiProperties(
        String baseUrl,
        String set,
        String metadataPrefix,
        int maxRecords,
        Duration requestDelay,
        String userAgent) {
}
