package com.example.opensample.service;

public class SampleDataNotFoundException extends RuntimeException {

    public SampleDataNotFoundException(long id) {
        super("Sample data with id " + id + " was not found");
    }
}
