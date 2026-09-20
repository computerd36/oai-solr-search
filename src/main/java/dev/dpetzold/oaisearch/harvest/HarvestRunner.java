package dev.dpetzold.oaisearch.harvest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("harvest")
class HarvestRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HarvestRunner.class);

    private final OaiHarvester harvester;

    HarvestRunner(OaiHarvester harvester) {
        this.harvester = harvester;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<OaiRecord> records = harvester.harvest();
        records.stream().findFirst().ifPresent(first -> log.info("first record: {}", first));
    }
}
