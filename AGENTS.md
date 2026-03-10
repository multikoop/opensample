# AGENTS

## Ziel
Diese Codebase wird nach fachlichen Modulen strukturiert, damit neue Features von Anfang an klar getrennt, leichter lesbar und wartbar bleiben.

## Struktur-Konvention fuer neue Features
- Lege neue Features immer als dediziertes Top-Level-Package unter `com.example.opensample.<feature>` an.
- Trenne innerhalb eines Features mindestens nach Verantwortungen:
  - `api` (Controller)
  - `service` (Business-Logik)
  - `config` (Feature-spezifische Konfiguration)
  - `api.dto` (Feature-spezifische Request/Response-Typen)
- Vermeide neue feature-spezifische Klassen in generischen Paketen wie `com.example.opensample.api`, `com.example.opensample.service` oder `com.example.opensample.config`.

## Bereits etablierte Module
- Cassandra: `com.example.opensample.cassandra.*`
- MariaDB/Liquibase: `com.example.opensample.mariadb.*`

## Regeln fuer Folge-Features
- Bei jedem neuen Beispiel (weitere Datenbank, Streaming-Variante, Integrationsbeispiel etc.) sofort eigenes Modul anlegen, z. B.:
  - `com.example.opensample.postgres.*`
  - `com.example.opensample.kafka.*`
- Tests spiegeln die gleiche Modulstruktur unter `src/test/java`.
- Nur wirklich gemeinsame, fachlich neutrale Bausteine bleiben im Root-Namespace `com.example.opensample.*` (z. B. App-Start, globale Exception-Behandlung, wirklich geteilte Utilities).

## Aenderungen im Bestand
- Beim Erweitern bestehender Features immer zuerst pruefen, ob Code in ein bestehendes Feature-Modul gehoert.
- Falls ein neues Thema dazukommt, nicht in bestehende Module hineinmischen, sondern neues Modul erstellen.
