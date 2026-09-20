# oai-solr-search

Suchprototyp für die Metadaten der Digitalisierten Sammlungen der
Staatsbibliothek zu Berlin. Ich hole die Daten über die offene
OAI-PMH-Schnittstelle, indexiere sie in Apache Solr und stelle sie über eine
facettierte Such-API bereit.

## Start

    docker compose up -d
    ./mvnw spring-boot:run

Solr läuft dann unter http://localhost:8983 mit dem Core `sbb`, die Anwendung
unter http://localhost:8080.

## Datenquelle

Die OAI-PMH-Schnittstelle der Digitalisierten Sammlungen liegt unter
<https://oai.sbb.berlin/oai>. Die ältere Adresse unter
`digital.staatsbibliothek-berlin.de/oai` leitet mit 301 dorthin weiter.

Voreingestellt ist das Set `jean.paul` mit 613 Datensätzen, klein genug für
einen Lauf von etwa zwanzig Sekunden. Nach oben begrenzt `oai.max-records`.
Zwischen zwei Seitenabrufen liegt eine halbe Sekunde Pause, und jede Anfrage
trägt einen eigenen User-Agent. Die Schnittstelle ist öffentlich und
unentgeltlich, da blättert man nicht mit voller Geschwindigkeit durch.

Harvest auslösen:

    ./mvnw spring-boot:run -Dspring-boot.run.profiles=harvest
