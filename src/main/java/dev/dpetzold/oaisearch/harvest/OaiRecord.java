package dev.dpetzold.oaisearch.harvest;

import java.util.List;

public record OaiRecord(
        String ppn,
        String title,
        List<String> creators,
        String date,
        List<String> subjects,
        List<String> languages,
        List<String> types,
        String url) {
}
