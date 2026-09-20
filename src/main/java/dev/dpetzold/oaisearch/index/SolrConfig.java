package dev.dpetzold.oaisearch.index;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SolrConfig {

    @Bean(destroyMethod = "close")
    SolrClient solrClient(SolrProperties properties) {
        return new HttpJdkSolrClient.Builder(properties.url()).build();
    }
}
