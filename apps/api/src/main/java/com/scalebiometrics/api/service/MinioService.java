package com.scalebiometrics.api.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name:scalebiometrics-images}")
    private String bucketName;

    public String uploadFile(String tenantId, String rid, MultipartFile file, int index) {
        try {
            String objectName = String.format("%s/%s/finger_%d_%s", tenantId, rid, index, file.getOriginalFilename());
            
            // Ensure bucket exists (this should ideally be done at startup or infrastructure level)
            // boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            // if (!found) {
            //     minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            // }

            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build());
            }

            log.info("Uploaded file to MinIO: {}", objectName);
            return objectName;

        } catch (Exception e) {
            log.error("Error uploading file to MinIO", e);
            throw new RuntimeException("Failed to upload file to storage", e);
        }
    }
}
