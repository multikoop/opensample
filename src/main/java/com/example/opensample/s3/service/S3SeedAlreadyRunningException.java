package com.example.opensample.s3.service;

public class S3SeedAlreadyRunningException extends RuntimeException {

    public S3SeedAlreadyRunningException() {
        super("Another S3 seed operation is already running");
    }
}
