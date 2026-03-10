package com.example.opensample.s3.service;

import com.example.opensample.s3.api.dto.S3SampleObjectResponse;
import com.example.opensample.s3.config.S3SampleProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class S3SampleDataService {

    private static final List<SeedFile> DEFAULT_SEED_FILES = List.of(
            new SeedFile("sample-note.txt", "text/plain"),
            new SeedFile("sample-guide.pdf", "application/pdf")
    );

    private final S3Client s3Client;
    private final S3SampleProperties properties;
    private final ReentrantLock seedLock = new ReentrantLock();

    public S3SampleDataService(S3Client s3Client, S3SampleProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    public void seedSampleData() {
        if (!seedLock.tryLock()) {
            throw new S3SeedAlreadyRunningException();
        }

        try {
            ensureBucketExists();
            for (SeedFile seedFile : DEFAULT_SEED_FILES) {
                uploadSeedFile(seedFile);
            }
        } finally {
            seedLock.unlock();
        }
    }

    public List<S3SampleObjectResponse> listObjects() {
        try {
            ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(validBucketName(properties.getBucket()))
                    .build());

            return response.contents().stream()
                    .map(item -> new S3SampleObjectResponse(
                            item.key(),
                            item.size(),
                            toUtcDateTime(item.lastModified())
                    ))
                    .sorted(Comparator.comparing(S3SampleObjectResponse::key))
                    .toList();
        } catch (NoSuchBucketException exception) {
            return List.of();
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return List.of();
            }
            throw exception;
        }
    }

    public S3DownloadedObject downloadObject(String objectKey) {
        String validObjectKey = validObjectKey(objectKey);

        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(validBucketName(properties.getBucket()))
                    .key(validObjectKey)
                    .build());

            String contentType = response.response().contentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            return new S3DownloadedObject(validObjectKey, contentType, response.asByteArray());
        } catch (NoSuchKeyException exception) {
            throw new S3SampleObjectNotFoundException(validObjectKey);
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new S3SampleObjectNotFoundException(validObjectKey);
            }
            throw exception;
        }
    }

    public boolean isUnavailable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SdkClientException) {
                return true;
            }
            if (current instanceof ConnectException) {
                return true;
            }
            if (current instanceof UnknownHostException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void ensureBucketExists() {
        String bucket = validBucketName(properties.getBucket());
        ListBucketsResponse response = s3Client.listBuckets();
        boolean bucketExists = response.buckets().stream().anyMatch(item -> bucket.equals(item.name()));
        if (!bucketExists) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
    }

    private void uploadSeedFile(SeedFile seedFile) {
        Path path = resolveSeedFile(seedFile.objectKey());
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read S3 seed file: " + path, exception);
        }

        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(validBucketName(properties.getBucket()))
                        .key(seedFile.objectKey())
                        .contentType(seedFile.contentType())
                        .build(),
                RequestBody.fromBytes(bytes));
    }

    private Path resolveSeedFile(String fileName) {
        if (properties.getSeedFilesDirectory() == null || properties.getSeedFilesDirectory().isBlank()) {
            throw new IllegalArgumentException("S3 seedFilesDirectory must not be blank");
        }
        return Path.of(properties.getSeedFilesDirectory().trim())
                .resolve(fileName)
                .normalize();
    }

    private String validBucketName(String bucketName) {
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalArgumentException("S3 bucket must not be blank");
        }

        String normalized = bucketName.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")) {
            throw new IllegalArgumentException(
                    "Invalid S3 bucket '" + bucketName + "'. Allowed: lowercase letters, numbers, dots, dashes"
            );
        }
        return normalized;
    }

    private String validObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("S3 object key must not be blank");
        }

        String normalized = objectKey.trim();
        if (normalized.contains("..") || normalized.startsWith("/")) {
            throw new IllegalArgumentException("Invalid S3 object key");
        }
        return normalized;
    }

    private LocalDateTime toUtcDateTime(Instant instant) {
        if (Objects.isNull(instant)) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private record SeedFile(String objectKey, String contentType) {
    }
}
