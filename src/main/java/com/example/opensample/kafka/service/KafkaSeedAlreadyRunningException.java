package com.example.opensample.kafka.service;

public class KafkaSeedAlreadyRunningException extends RuntimeException {

    public KafkaSeedAlreadyRunningException() {
        super("Kafka seed is already running");
    }
}
