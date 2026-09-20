package dev.dpetzold.oaisearch.harvest;

import dev.dpetzold.oaisearch.index.SolrIndexer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

// harvest profile only, so a normal start does not hit the API
@Component
@Profile("harvest")
class HarvestRunner implements ApplicationRunner {

    private final OaiHarvester harvester;
    private final SolrIndexer indexer;

    HarvestRunner(OaiHarvester harvester, SolrIndexer indexer) {
        this.harvester = harvester;
        this.indexer = indexer;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<OaiRecord> records = harvester.harvest();
        indexer.index(records);
    }
}
