package com.thechat.object_storage;

import java.net.URI;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(ObjectStorageProperties.class)
public class ObjectStorageConfig {

        @Bean(destroyMethod = "close")
        S3Client s3Client(ObjectStorageProperties properties) {
                AwsBasicCredentials credentials = AwsBasicCredentials.create(
                                properties.accessKeyId(),
                                properties.secretAccessKey());

                return S3Client.builder()
                                .endpointOverride(URI.create(properties.endpoint()))
                                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                                .region(Region.of("auto"))
                                .serviceConfiguration(S3Configuration.builder()
                                                .pathStyleAccessEnabled(true)
                                                .build())
                                .build();
        }

        @Bean(destroyMethod = "close")
        S3Presigner s3Presigner(ObjectStorageProperties properties) {
                AwsBasicCredentials credentials = AwsBasicCredentials.create(
                                properties.accessKeyId(),
                                properties.secretAccessKey());

                return S3Presigner.builder()
                                .endpointOverride(URI.create(properties.endpoint()))
                                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                                .region(Region.of("auto"))
                                .serviceConfiguration(S3Configuration.builder()
                                                .pathStyleAccessEnabled(true)
                                                .build())
                                .build();
        }

        @Bean
        CloudflareR2Client cloudflareR2Client(
                        S3Presigner s3Presigner,
                        S3Client s3Client,
                        ObjectStorageProperties properties) {
                return new CloudflareR2Client(s3Presigner, s3Client, properties);
        }
}
