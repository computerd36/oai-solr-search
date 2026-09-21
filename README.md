# oai-solr-search

[![build](https://github.com/computerd36/oai-solr-search/actions/workflows/ci.yml/badge.svg)](https://github.com/computerd36/oai-solr-search/actions/workflows/ci.yml)

Suchprototyp für die Metadaten der Digitalisierten Sammlungen der
Staatsbibliothek zu Berlin. Ich hole die Daten über die offene
OAI-PMH-Schnittstelle, indexiere sie in Apache Solr und stelle sie über eine
facettierte Such-API bereit.

## Start

    docker compose up -d
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=harvest
    cd frontend && npm install && npm run dev

Der zweite Befehl startet die Anwendung und holt einmalig die Daten. Bei
späteren Starts reicht `./mvnw spring-boot:run` ohne profiles=harvest, sonst 
wird erneut geharvestet.

Der dritte Befehl startet die React-Anwendung, die auf die API zugreift. 

Solr läuft dann unter http://localhost:8983 mit dem Core `sbb`, die Anwendung
unter http://localhost:8080, die Oberfläche unter http://localhost:5173.

## Architektur

Vier Teile. Der Harvester holt die Datensätze über OAI-PMH und blättert sich
über `resumptionToken` durch die Seiten. Der Indexer bildet sie auf die
dynamischen Felder von Solr ab und schreibt sie in Blöcken; Dokument-ID ist die
PPN, ein zweiter Lauf überschreibt also, statt zu verdoppeln. Die Such-API baut
daraus eine Solr-Query mit Facetten und gibt Treffer und Zählungen als JSON
zurück. 

## Datenquelle

Die OAI-PMH-Schnittstelle der Digitalisierten Sammlungen liegt unter
<https://oai.sbb.berlin/oai>. Die ältere Adresse unter
`digital.staatsbibliothek-berlin.de/oai` leitet dorthin weiter.

Voreingestellt ist das Set `jean.paul` mit 613 Datensätzen, klein genug für
einen Lauf von etwa zwanzig Sekunden. Nach oben begrenzt via `oai.max-records`.
Zwischen zwei Seitenabrufen liegt eine halbe Sekunde Pause, und jede Anfrage
trägt einen eigenen User-Agent. 

Ein anderes Set setzt man über `oai.set` in der `application.yml`. Das größte
Set ist `all` mit gut 244.000 Datensätzen; allein die Pausen summieren sich dort auf
rund vierzig Minuten, was den Endpunkt für diesen Prototyp unnötig belastet. 

## Suche

`GET /api/search` liefert Treffer und Facetten als JSON. Parameter sind `q` für
den Freitext, `page` und `size` für die Seitenzahl, und je Facette `creator`,
`subject`, `language` und `year`. `size` ist auf 100 begrenzt.

    curl 'localhost:8080/api/search?q=Teufel&size=3'
    curl 'localhost:8080/api/search?language=fre'
    curl 'localhost:8080/api/search?year=1790&creator=Jean+Paul'
    curl 'localhost:8080/api/search?q=Satire&page=1&size=5'

Die Antwort enthält `total`, `items` und `facets`. Mehrere Werte derselben
Facette werden mit ODER verknüpft, verschiedene Facetten mit UND.

Filter gehen als `fq` an Solr und nicht in die Suchanfrage selbst. Sie
entscheiden nur, welche Dokumente überhaupt in Frage kommen, und verschieben die
Rangfolge nicht. Wer nach Sprache filtert, will die Auswahl einschränken und
nicht seltene Sprachen nach oben sortiert bekommen.

Die API ist außerdem unter <http://localhost:8080/swagger-ui.html>
dokumentiert.

## Oberfläche

Eine Seite mit Suchfeld, Facettenspalte und Trefferliste, gebaut mit Vite,
React und TypeScript. Der Dev-Server proxyt `/api` auf Port 8080, dadurch
sieht der Browser eine einzige Herkunft und CORS ist kein Thema. 

Bewusst schlank: kein State-Management, keine Komponenten- oder
CSS-Bibliothek, kein Routing. Der Schwerpunkt dieses Prototyps liegt auf
Harvesting, Indexierung und Such-API. Die Oberfläche ist dazu da, das
sichtbar und bedienbar zu machen, und nicht mehr. Ein Frontend, das über
diesen Zweck hinausgeht, würde ich anders gestalten.

## Nicht enthalten

- **Keine Authentifizierung**, weder vor der API noch vor dem Harvest. Der
  Harvest hängt deshalb an einem Profil und nicht an einem offenen Endpunkt.
- **Keine inkrementelle Aktualisierung.** Jeder Lauf holt das ganze Set.
  Nachrüsten hieße, den letzten `datestamp` zu merken und als `from` mitzugeben.
- **Kein eigenes Solr-Schema.** Der Index nutzt die dynamischen Felder des
  Default-Configsets, was ein doppeltes Ablegen der durchsuchbaren Werte kostet.
- **Keine Digitalisate.** Verlinkt wird der Resolver aus `dc:identifier`, eine
  IIIF-Anbindung gibt es nicht.
- **Kein Deployment.** Läuft lokal über Compose und das Maven-Plugin; für einen
  Server bräuchte es ein Image und eine Solr-Instanz daneben.
