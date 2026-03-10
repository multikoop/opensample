package com.example.opensample.s3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.s3")
public class S3SampleProperties {

    private String endpoint = "http://localhost:4566";
    private String region = "eu-central-1";
    private String accessKey = "test";
    private String secretKey = "test";
    private String bucket = "sample-bucket";
    private boolean pathStyleAccessEnabled = true;
    private long requestTimeoutMs = 5000;
    private String seedFilesDirectory = "src/test/resources/s3/sample-bucket";

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public boolean isPathStyleAccessEnabled() {
        return pathStyleAccessEnabled;
    }

    public void setPathStyleAccessEnabled(boolean pathStyleAccessEnabled) {
        this.pathStyleAccessEnabled = pathStyleAccessEnabled;
    }

    public long getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(long requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public String getSeedFilesDirectory() {
        return seedFilesDirectory;
    }

    public void setSeedFilesDirectory(String seedFilesDirectory) {
        this.seedFilesDirectory = seedFilesDirectory;
    }
}
