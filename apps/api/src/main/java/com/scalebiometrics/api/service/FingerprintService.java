package com.scalebiometrics.api.service;

import com.scalebiometrics.api.repository.FingerprintRepository;
import com.scalebiometrics.core.domain.Fingerprint;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FingerprintService {

    private final MinioClient minioClient;
    private final FingerprintRepository fingerprintRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public Fingerprint uploadFingerprint(String rid, int fingerIndex, MultipartFile file) {
        UUID fingerprintId = UUID.randomUUID();
        String objectName = rid + "/" + fingerIndex + "_" + fingerprintId + ".iso";

        try {
            // 1. Upload to MinIO
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            // 2. Save Metadata to DB
            Fingerprint fingerprint = Fingerprint.builder()
                    .id(fingerprintId)
                    .rid(rid)
                    .fingerIndex(fingerIndex)
                    .storagePath(objectName)
                    .createdAt(LocalDateTime.now())
                    // Template and embedding will be processed by Worker
                    .build();

            Fingerprint saved = fingerprintRepository.save(fingerprint);

            // 3. Publish Event to Kafka
            // We publish the ID and path so workers can download, extract template, and index
            kafkaTemplate.send("fingerprint-ingestion", saved);

            log.info("Fingerprint uploaded and event published. ID: {}", fingerprintId);
            return saved;

        } catch (Exception e) {
            log.error("Error uploading fingerprint", e);
            throw new RuntimeException("Failed to upload fingerprint", e);
        }
    }
}
