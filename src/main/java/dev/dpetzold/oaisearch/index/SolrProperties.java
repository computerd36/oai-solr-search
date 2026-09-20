package dev.dpetzold.oaisearch.index;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "solr")
public record SolrProperties(String url, String core) {
}
