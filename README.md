# oai-solr-search

Suchprototyp für die Metadaten der Digitalisierten Sammlungen der
Staatsbibliothek zu Berlin. Ich hole die Daten über die offene
OAI-PMH-Schnittstelle, indexiere sie in Apache Solr und stelle sie über eine
facettierte Such-API bereit.

## Start

    docker compose up -d
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=harvest
    cd frontend && npm install && npm run dev

Der zweite Befehl startet die Anwendung und holt einmalig die Daten. Bei
späteren Starts reicht `./mvnw spring-boot:run` ohne Profil, sonst wird jedes
Mal neu geharvestet.

Solr läuft dann unter http://localhost:8983 mit dem Core `sbb`, die Anwendung
unter http://localhost:8080, die Oberfläche unter http://localhost:5173.

## Datenquelle

Die OAI-PMH-Schnittstelle der Digitalisierten Sammlungen liegt unter
<https://oai.sbb.berlin/oai>. Die ältere Adresse unter
`digital.staatsbibliothek-berlin.de/oai` leitet mit 301 dorthin weiter.

Voreingestellt ist das Set `jean.paul` mit 613 Datensätzen, klein genug für
einen Lauf von etwa zwanzig Sekunden. Nach oben begrenzt `oai.max-records`.
Zwischen zwei Seitenabrufen liegt eine halbe Sekunde Pause, und jede Anfrage
trägt einen eigenen User-Agent. Die Schnittstelle ist öffentlich und
unentgeltlich, da blättert man nicht mit voller Geschwindigkeit durch.

## Suche

`GET /api/search` liefert Treffer und Facetten als JSON. Parameter sind `q` für
den Freitext, `page` und `size` für die Seitenzahl, und je Facette `creator`,
`subject`, `language` und `year`. `size` ist auf 100 begrenzt. Filter landen als
`fq` in der Anfrage und beeinflussen die Bewertung der Treffer nicht.

    curl 'localhost:8080/api/search?q=Teufel&size=3'
    curl 'localhost:8080/api/search?language=fre'
    curl 'localhost:8080/api/search?year=1790&creator=Jean+Paul'
    curl 'localhost:8080/api/search?q=Satire&page=1&size=5'

Die Antwort enthält `total`, `items` und `facets`. Mehrere Werte derselben
Facette werden mit ODER verknüpft, verschiedene Facetten mit UND.

Die API ist außerdem unter <http://localhost:8080/swagger-ui.html>
dokumentiert.

## Oberfläche

Eine Seite mit Suchfeld, Facettenspalte und Trefferliste, gebaut mit Vite,
React und TypeScript. Der Dev-Server proxyt `/api` auf Port 8080, dadurch
sieht der Browser eine einzige Herkunft und CORS wird kein Thema.

Bewusst schlank: kein State-Management, keine Komponenten- oder
CSS-Bibliothek, kein Routing. Der Schwerpunkt dieses Prototyps liegt auf
Harvesting, Indexierung und Such-API. Die Oberfläche ist dazu da, das
sichtbar und bedienbar zu machen, und nicht mehr. Ein Frontend, das über
diesen Zweck hinausgeht, würde ich anders schneiden.

Auf Barrierefreiheit habe ich trotzdem geachtet: das Suchfeld hat ein echtes
`<label>` und einen über `aria-describedby` verknüpften Hinweis, die
Facettengruppen stecken in `<fieldset>` mit `<legend>`, die Trefferzahl liegt
in einer Region mit `aria-live="polite"`, die Trefferliste ist eine `<ul>`,
und der Fokus bleibt überall sichtbar.
