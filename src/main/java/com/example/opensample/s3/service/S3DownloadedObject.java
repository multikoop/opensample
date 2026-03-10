package com.example.opensample.s3.service;

public record S3DownloadedObject(
        String fileName,
        String contentType,
        byte[] content
) {
}
