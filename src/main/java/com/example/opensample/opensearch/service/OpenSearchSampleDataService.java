package com.example.opensample.opensearch.service;

import com.example.opensample.opensearch.api.dto.OpenSearchSampleDocumentResponse;
import com.example.opensample.opensearch.config.OpenSearchSampleProperties;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class OpenSearchSampleDataService {

    private static final List<SeedDocument> DEFAULT_DOCUMENTS = List.of(
            new SeedDocument(
                    "stellenangebot-1",
                    """
                    {
                      "type": "Stellengesuch",
                      "title": "Java Entwickler (m/w/d)",
                      "description": "Erfahrung mit Spring Boot, REST APIs und Docker.",
                      "location": "Berlin",
                      "timestamp": "2026-03-01T08:00:00Z"
                    }
                    """
            ),
            new SeedDocument(
                    "berufsinformation-1",
                    """
                    {
                      "type": "Berufsinformationen",
                      "title": "Berufsbild Data Engineer",
                      "description": "Aufgaben: Datenpipelines, Modellierung und Monitoring.",
                      "timestamp": "2026-03-02T09:30:00Z"
                    }
                    """
            ),
            new SeedDocument(
                    "praktikum-1",
                    """
                    {
                      "type": "Praktikum",
                      "title": "Praktikum IT-Support",
                      "description": "6 Monate, Einstieg in Support, Ticketing und Automatisierung.",
                      "location": "Hamburg",
                      "timestamp": "2026-03-03T10:15:00Z"
                    }
                    """
            )
    );

    private final OpenSearchSampleProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ReentrantLock seedLock = new ReentrantLock();

    public OpenSearchSampleDataService(OpenSearchSampleProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(validRequestTimeout(properties.getRequestTimeoutMs())))
                .build();
    }

    public void seedSampleData() {
        if (!seedLock.tryLock()) {
            throw new OpenSearchSeedAlreadyRunningException();
        }

        try {
            String index = validIndex(properties.getIndex());
            ensureIndexExists(index);
            for (SeedDocument document : DEFAULT_DOCUMENTS) {
                putDocument(index, document.id(), document.sourceJson());
            }
        } finally {
            seedLock.unlock();
        }
    }

    public List<OpenSearchSampleDocumentResponse> listDocuments(String queryText) {
        String index = validIndex(properties.getIndex());
        HttpResponse<String> response = send(
                request("/" + encode(index) + "/_search")
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .method("POST", HttpRequest.BodyPublishers.ofString(searchPayload(validMaxDocuments(properties.getMaxDocuments()), queryText)))
                        .build()
        );

        if (response.statusCode() == HttpStatus.NOT_FOUND.value()) {
            return List.of();
        }
        ensureSuccess(response, "OpenSearch search failed");

        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode hits = root.path("hits").path("hits");
            if (!hits.isArray()) {
                return List.of();
            }

            List<OpenSearchSampleDocumentResponse> result = new ArrayList<>();
            for (JsonNode hit : hits) {
                JsonNode source = hit.path("_source");
                result.add(new OpenSearchSampleDocumentResponse(
                        hit.path("_index").asText(null),
                        hit.path("_id").asText(null),
                        hit.path("_score").isNumber() ? hit.path("_score").asDouble() : null,
                        source.path("title").asText("-"),
                        toPrettyJson(source),
                        extractTimestamp(source)
                ));
            }

            return result;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("OpenSearch response could not be parsed", exception);
        }
    }

    public List<OpenSearchSampleDocumentResponse> listDocuments() {
        return listDocuments(null);
    }

    public boolean isUnavailable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof HttpTimeoutException
                    || current instanceof ConnectException
                    || current instanceof UnknownHostException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void ensureIndexExists(String index) {
        HttpResponse<String> existsResponse = send(
                request("/" + encode(index))
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .build()
        );

        if (existsResponse.statusCode() == HttpStatus.OK.value()) {
            return;
        }
        if (existsResponse.statusCode() != HttpStatus.NOT_FOUND.value()) {
            ensureSuccess(existsResponse, "OpenSearch index check failed");
            return;
        }

        HttpResponse<String> createResponse = send(
                request("/" + encode(index))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString("{\"settings\":{\"number_of_shards\":1,\"number_of_replicas\":0}}"))
                        .build()
        );

        if (createResponse.statusCode() == HttpStatus.BAD_REQUEST.value() && createResponse.body().contains("resource_already_exists_exception")) {
            return;
        }
        ensureSuccess(createResponse, "OpenSearch index creation failed");
    }

    private void putDocument(String index, String id, String sourceJson) {
        HttpResponse<String> response = send(
                request("/" + encode(index) + "/_doc/" + encode(id) + "?refresh=wait_for")
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(sourceJson))
                        .build()
        );
        ensureSuccess(response, "OpenSearch document index failed");
    }

    private HttpRequest.Builder request(String pathAndQuery) {
        URI uri = URI.create(validEndpoint(properties.getEndpoint()) + pathAndQuery);
        return HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMillis(validRequestTimeout(properties.getRequestTimeoutMs())))
                .header("Accept", "application/json");
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new IllegalStateException("OpenSearch request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenSearch request interrupted", exception);
        }
    }

    private void ensureSuccess(HttpResponse<String> response, String message) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(message + " (HTTP " + response.statusCode() + ")");
        }
    }

    private String toPrettyJson(JsonNode source) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(source);
        } catch (JsonProcessingException exception) {
            return source.toString();
        }
    }

    private LocalDateTime extractTimestamp(JsonNode source) {
        JsonNode timestampNode = source.path("timestamp");
        if (timestampNode.isMissingNode() || timestampNode.isNull()) {
            return null;
        }

        String raw = timestampNode.asText();
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(raw).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(raw), ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String searchPayload(int maxDocuments, String queryText) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("size", maxDocuments);

        String normalizedQuery = normalizeQuery(queryText);
        if (normalizedQuery == null) {
            root.set("query", objectMapper.createObjectNode().set("match_all", objectMapper.createObjectNode()));
            ArrayNode sort = root.putArray("sort");
            ObjectNode timestampSort = objectMapper.createObjectNode();
            timestampSort.set("timestamp", objectMapper.createObjectNode().put("order", "desc"));
            sort.add(timestampSort);
        } else {
            ObjectNode queryNode = objectMapper.createObjectNode();
            ObjectNode multiMatchNode = objectMapper.createObjectNode();
            multiMatchNode.put("query", normalizedQuery);
            ArrayNode fields = multiMatchNode.putArray("fields");
            fields.add("title^3");
            fields.add("description");
            fields.add("type");
            fields.add("location");
            multiMatchNode.put("operator", "or");
            queryNode.set("multi_match", multiMatchNode);
            root.set("query", queryNode);
        }

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("OpenSearch search payload could not be generated", exception);
        }
    }

    private String normalizeQuery(String queryText) {
        if (queryText == null) {
            return null;
        }
        String normalized = queryText.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String validEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("OpenSearch endpoint must not be blank");
        }
        return endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
    }

    private String validIndex(String index) {
        if (index == null || index.isBlank()) {
            throw new IllegalArgumentException("OpenSearch index must not be blank");
        }
        String normalized = index.trim();
        if (!normalized.matches("[a-z0-9._-]+")) {
            throw new IllegalArgumentException("Invalid OpenSearch index '" + index + "'");
        }
        return normalized;
    }

    private long validRequestTimeout(long requestTimeoutMs) {
        if (requestTimeoutMs < 100) {
            throw new IllegalArgumentException("OpenSearch requestTimeoutMs must be >= 100");
        }
        return requestTimeoutMs;
    }

    private int validMaxDocuments(int maxDocuments) {
        if (maxDocuments < 1) {
            throw new IllegalArgumentException("OpenSearch maxDocuments must be >= 1");
        }
        return maxDocuments;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record SeedDocument(String id, String sourceJson) {
    }
}
