package dev.dpetzold.oaisearch.harvest;

import java.util.List;

record OaiResponse(List<OaiRecord> records, String resumptionToken) {

    boolean hasMore() {
        return resumptionToken != null && !resumptionToken.isBlank();
    }
}
