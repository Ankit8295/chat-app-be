package com.thechat.object_storage;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Cloudflare R2 helper: browser direct upload/download via PRESIGNED URLs,
 * plus server-side delete for orphan cleanup. Bytes for upload/download never
 * pass through the backend. Public profile reads use {@link #publicUrl(String)}.
 */
public class CloudflareR2Client {

    private final S3Presigner presigner;
    private final S3Client s3Client;
    private final ObjectStorageProperties properties;

    public CloudflareR2Client(
            S3Presigner presigner,
            S3Client s3Client,
            ObjectStorageProperties properties) {
        this.presigner = presigner;
        this.s3Client = s3Client;
        this.properties = properties;
    }

    /**
     * Builds a stable public URL for profile (and similar) objects.
     * Call this on the backend only — never expose publicBaseUrl for FE concatenation.
     */
    public String publicUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        String base = properties.publicBaseUrl().replaceAll("/+$", "");
        String key = objectKey.replaceAll("^/+", "");
        return base + "/" + key;
    }

    public String createPresignedPutUrl(String objectKey, String contentType) {
        PutObjectRequest.Builder putObjectBuilder = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey);

        if (contentType != null && !contentType.isBlank()) {
            putObjectBuilder.contentType(contentType);
        }

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(properties.putUrlTtl())
                .putObjectRequest(putObjectBuilder.build())
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    public String createPresignedGetUrl(String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(properties.getUrlTtl())
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    public void deleteObject(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build());
    }

    public long putUrlTtlSeconds() {
        return properties.putUrlTtl().toSeconds();
    }

    public long getUrlTtlSeconds() {
        return properties.getUrlTtl().toSeconds();
    }
}
