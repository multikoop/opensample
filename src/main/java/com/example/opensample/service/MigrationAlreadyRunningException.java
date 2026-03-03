package com.example.opensample.service;

public class MigrationAlreadyRunningException extends RuntimeException {

    public MigrationAlreadyRunningException() {
        super("A migration run is already in progress");
    }
}
