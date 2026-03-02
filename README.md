# OpenSample

Beispielanwendung mit:
- Java 21
- Spring Boot 3.4.3
- MariaDB
- Liquibase (Schema + Seeddaten)
- Thymeleaf
- REST API + OpenAPI
- Testcontainers (Integrationstest)
- Maven

## Features

- Liquibase fuehrt beim ersten Start die Migrationen aus.
- Datenmodell `sample_data` mit 5 Seed-Datensaetzen.
- Thymeleaf Startseite mit zentral wiederverwendbarer Tab-Komponente.
- Tabs:
  - Startseite
  - MariaDB (zeigt tabellarisch DB-Inhalte)
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

## Voraussetzungen

- `JAVA_HOME=/opt/homebrew/opt/openjdk@21`
- Docker CLI + Docker Daemon
- Maven

## Lokal starten (MariaDB via Docker Compose)

```bash
docker compose up -d mariadb

export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"

# Optional DB overrides
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=opensample
export DB_USER=opensample
export DB_PASSWORD=opensample

mvn spring-boot:run
```

App URLs:
- `http://localhost:8080/`
- `http://localhost:8080/mariadb`
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
- S3
- Cassandra
- Kafka
