# OpenSample

Beispielanwendung mit:
- Java 21
- Spring Boot 3.4.3
- MariaDB
- Cassandra
- S3 (via LocalStack)
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
- Datenmodell `sample_data` mit 5 Seed-Datensaetzen.
- S3 Beispieldateien (TXT + PDF) werden in `sample-bucket` hochgeladen.
- Thymeleaf Startseite mit zentral wiederverwendbarer Tab-Komponente.
- Tabs:
  - Startseite
  - MariaDB (zeigt tabellarisch DB-Inhalte)
  - Cassandra (zeigt tabellarisch Cassandra-Inhalte)
  - S3 (zeigt Bucket-Objekte und Download-Links)
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
- S3 API:
  - `GET /api/v1/s3/objects` listet Objekte aus dem Sample-Bucket
  - `GET /api/v1/s3/objects/download?key=<object-key>` laedt ein Objekt herunter

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

## Voraussetzungen

- `JAVA_HOME=/opt/homebrew/opt/openjdk@21`
- Docker CLI + Docker Daemon
- Maven

## Lokal starten (MariaDB + Cassandra + S3/LocalStack via Docker Compose)

```bash
docker compose up -d mariadb cassandra localstack

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

# Optional S3 overrides (LocalStack)
export S3_ENDPOINT=http://localhost:4566
export S3_REGION=eu-central-1
export S3_ACCESS_KEY=test
export S3_SECRET_KEY=test
export S3_BUCKET=sample-bucket
export S3_SEED_FILES_DIRECTORY=src/test/resources/s3/sample-bucket

mvn spring-boot:run
```

Danach (falls DB spaeter gestartet wurde) Migration/Seed manuell ausloesen:

```bash
curl -X POST http://localhost:8080/api/v1/admin/db/migrate
curl -X POST http://localhost:8080/api/v1/admin/cassandra/seed
curl -X POST http://localhost:8080/api/v1/admin/s3/seed
```

App URLs:
- `http://localhost:8080/`
- `http://localhost:8080/mariadb`
- `http://localhost:8080/cassandra`
- `http://localhost:8080/s3`
- `http://localhost:8080/streaming`
- `http://localhost:8080/swagger-ui.html`

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

Die Tab-Navigation ist zentral als Thymeleaf-Fragment angelegt (`templates/fragments/tabs.html`) und kann fuer weitere Bereiche erweitert werden, z. B.:
- Kafka

## Code-Struktur (kurz)

- Neue Themen werden als eigenes Feature-Modul unter `com.example.opensample.<feature>.*` angelegt (z. B. `cassandra`, `mariadb`).
- Nur fachlich neutrale, geteilte Bausteine bleiben im Root-Namespace `com.example.opensample.*`.
- Details und verbindliche Regeln stehen in `AGENTS.md`.
