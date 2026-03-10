package com.example.opensample.service;

public class CassandraSeedAlreadyRunningException extends RuntimeException {

    public CassandraSeedAlreadyRunningException() {
        super("A Cassandra seed run is already in progress");
    }
}
