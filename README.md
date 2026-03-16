# OpenSample

Beispielanwendung mit:
- Java 21
- Spring Boot 3.4.3
- MariaDB
- Cassandra
- S3 (via LocalStack)
- Kafka (via Redpanda)
- OpenSearch
- Liquibase (Schema + Seeddaten)
- Thymeleaf
- REST API + OpenAPI
- Testcontainers (Integrationstest)
- Maven

## Features

- Anwendung startet auch dann, wenn MariaDB noch nicht erreichbar ist.
- Liquibase-Migration wird manuell gestartet (API oder Startseiten-Button).
- Cassandra Schema+Seed wird manuell gestartet (API oder Startseiten-Button).
- S3 Bucket+Seed wird manuell gestartet (API oder Startseiten-Button).
- Kafka Topic+Seed wird manuell gestartet (API oder Startseiten-Button).
- OpenSearch Index+Seed wird manuell gestartet (API oder Startseiten-Button).
- Datenmodell `sample_data` mit 5 Seed-Datensaetzen.
- S3 Beispieldateien (TXT + PDF) werden in `sample-bucket` hochgeladen.
- Kafka Beispieldaten werden als 3 JSON-Events in ein Sample-Topic geschrieben.
- OpenSearch Beispieldaten werden als 3 JSON-Dokumente in `sample-index` geschrieben.
- Thymeleaf Startseite mit zentral wiederverwendbarer Tab-Komponente.
- Tabs:
  - Startseite
  - MariaDB (zeigt tabellarisch DB-Inhalte)
  - Cassandra (zeigt tabellarisch Cassandra-Inhalte)
  - S3 (zeigt Bucket-Objekte und Download-Links)
  - Kafka (zeigt Topic-Events und Detailansicht je Event)
  - OpenSearch (zeigt Index-Dokumente und Detailansicht je Dokument)
  - Streaming (aktuell leer)
- REST API:
  - `GET /api/v1/sample-data`
  - `GET /api/v1/sample-data/{id}`
- Health endpoint:
  - `GET /mt/health` liefert 200.
- OpenAPI:
  - JSON: `/v3/api-docs`
  - YAML: `/v3/api-docs.yaml`
  - UI: `/swagger-ui.html`
- Admin API:
  - `POST /api/v1/admin/db/migrate` startet Liquibase-Migration manuell
  - `POST /api/v1/admin/cassandra/seed` erstellt Cassandra Keyspace/Tabelle und Seed-Daten
  - `POST /api/v1/admin/s3/seed` erstellt S3 Bucket und laedt 2 Beispieldateien hoch
  - `POST /api/v1/admin/kafka/seed` erstellt Kafka Topic und publiziert 3 Beispiel-Events
  - `POST /api/v1/admin/opensearch/seed` erstellt OpenSearch Index und schreibt 3 Beispiel-Dokumente
- S3 API:
  - `GET /api/v1/s3/objects` listet Objekte aus dem Sample-Bucket
  - `GET /api/v1/s3/objects/download?key=<object-key>` laedt ein Objekt herunter
- Kafka API:
  - `GET /api/v1/kafka/events` listet Events aus dem Sample-Topic
- OpenSearch API:
  - `GET /api/v1/opensearch/documents` listet Dokumente aus dem Sample-Index
  - `GET /api/v1/opensearch/documents?q=<suchbegriff>` filtert Dokumente per einfacher Volltextsuche

## Datenmodell

Tabelle `sample_data`:
- `id` (BIGINT, PK, auto increment)
- `name` (VARCHAR(120), not null)
- `description` (VARCHAR(400), not null)
- `created_at` (DATETIME, not null)

Migrationen liegen unter:
- `src/main/resources/db/changelog/db.changelog-master.yaml`
- `src/main/resources/db/changelog/db.changelog-001-create-sample-data.yaml`
- `src/main/resources/db/changelog/db.changelog-002-seed-sample-data.yaml`

Cassandra Seed ist als einfacher Lauf ohne State-Tracking implementiert:
- Keyspace `opensample` (konfigurierbar via `CASSANDRA_KEYSPACE`)
- Tabelle `sample_data` (PK: `id`)
- 5 Beispielzeilen per Upsert

S3 Seed ist als einfacher Lauf ohne State-Tracking implementiert:
- Bucket `sample-bucket` (konfigurierbar via `S3_BUCKET`)
- Upload von 2 Dateien aus `src/test/resources/s3/sample-bucket`:
  - `sample-note.txt`
  - `sample-guide.pdf`

Kafka Seed ist als einfacher Lauf ohne State-Tracking implementiert:
- Topic `sample-account-events` (konfigurierbar via `KAFKA_TOPIC`)
- 3 JSON-Events vom Typ `AccountFreigeschaltet`

OpenSearch Seed ist als einfacher Lauf ohne State-Tracking implementiert:
- Index `sample-index` (konfigurierbar via `OPENSEARCH_INDEX`)
- 3 JSON-Dokumente: `Stellengesuch`, `Berufsinformationen`, `Praktikum`

## Voraussetzungen

- `JAVA_HOME=/opt/homebrew/opt/openjdk@21`
- Docker CLI + Docker Daemon
- Maven

## Lokal starten (MariaDB + Cassandra + S3/LocalStack + Kafka/Redpanda + OpenSearch via Docker Compose)

```bash
docker compose up -d mariadb cassandra localstack redpanda opensearch

# Optional mit OpenSearch Dashboards
# docker compose up -d opensearch-dashboards

# Optional: Bitnami Cassandra (mit User/Password), startet nur per Profile
# docker compose --profile bitnami-cassandra up -d cassandra-bitnami

export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"

# Optional DB overrides
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=opensample
export DB_USER=opensample
export DB_PASSWORD=opensample

# Optional Cassandra overrides
export CASSANDRA_HOST=localhost
export CASSANDRA_PORT=9042
export CASSANDRA_DATACENTER=datacenter1
export CASSANDRA_KEYSPACE=opensample

# Nur fuer bitnami-cassandra (Auth aktiviert)
# export CASSANDRA_HOST=localhost
# export CASSANDRA_PORT=9142
# export CASSANDRA_USER=cassandra
# export CASSANDRA_PASSWORD=cassandra

# Optional S3 overrides (LocalStack)
export S3_ENDPOINT=http://localhost:4566
export S3_REGION=eu-central-1
export S3_ACCESS_KEY=test
export S3_SECRET_KEY=test
export S3_BUCKET=sample-bucket
export S3_SEED_FILES_DIRECTORY=src/test/resources/s3/sample-bucket

# Optional Kafka overrides (Redpanda)
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export KAFKA_TOPIC=sample-account-events
export KAFKA_TOPIC_PARTITIONS=1
export KAFKA_TOPIC_REPLICATION_FACTOR=1
export KAFKA_REQUEST_TIMEOUT_MS=5000
export KAFKA_POLL_TIMEOUT_MS=700
export KAFKA_MAX_EVENTS=250

# Optional OpenSearch overrides
export OPENSEARCH_ENDPOINT=http://localhost:9200
export OPENSEARCH_INDEX=sample-index
export OPENSEARCH_REQUEST_TIMEOUT_MS=5000
export OPENSEARCH_MAX_DOCUMENTS=250
# Seit OpenSearch 2.12+ beim Containerstart erforderlich (auch im Demo-Setup)
export OPENSEARCH_INITIAL_ADMIN_PASSWORD=OpenSampleAdmin123!

mvn spring-boot:run
```

Danach (falls DB spaeter gestartet wurde) Migration/Seed manuell ausloesen:

```bash
curl -X POST http://localhost:8080/api/v1/admin/db/migrate
curl -X POST http://localhost:8080/api/v1/admin/cassandra/seed
curl -X POST http://localhost:8080/api/v1/admin/s3/seed
curl -X POST http://localhost:8080/api/v1/admin/kafka/seed
curl -X POST http://localhost:8080/api/v1/admin/opensearch/seed
```

App URLs:
- `http://localhost:8080/`
- `http://localhost:8080/mariadb`
- `http://localhost:8080/cassandra`
- `http://localhost:8080/s3`
- `http://localhost:8080/kafka`
- `http://localhost:8080/opensearch`
- `http://localhost:8080/streaming`
- `http://localhost:8080/swagger-ui.html`
- `http://localhost:5601` (optional OpenSearch Dashboards)

## Tests

Standard-Tests (ohne Docker-Abhaengigkeit):

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"

mvn test
```

Optionaler Integrationstest mit Testcontainers (MariaDB-Container):

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
export RUN_TESTCONTAINERS=true

mvn test
```

## Geplanter Ausbau

Die Tab-Navigation ist zentral als Thymeleaf-Fragment angelegt (`templates/fragments/tabs.html`) und kann fuer weitere Bereiche erweitert werden (z. B. weitere Streaming- oder Queue-Beispiele).

## Code-Struktur (kurz)

- Neue Themen werden als eigenes Feature-Modul unter `com.example.opensample.<feature>.*` angelegt (z. B. `cassandra`, `mariadb`).
- Nur fachlich neutrale, geteilte Bausteine bleiben im Root-Namespace `com.example.opensample.*`.
- Details und verbindliche Regeln stehen in `AGENTS.md`.
