package com.example.opensample.s3.service;

public class S3SampleObjectNotFoundException extends RuntimeException {

    public S3SampleObjectNotFoundException(String objectKey) {
        super("S3 object with key '" + objectKey + "' was not found");
    }
}
