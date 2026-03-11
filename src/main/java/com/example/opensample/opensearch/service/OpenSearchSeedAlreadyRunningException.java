package com.example.opensample.opensearch.service;

public class OpenSearchSeedAlreadyRunningException extends RuntimeException {

    public OpenSearchSeedAlreadyRunningException() {
        super("OpenSearch seed is already running");
    }
}
