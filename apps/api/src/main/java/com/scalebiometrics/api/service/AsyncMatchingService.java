package com.scalebiometrics.api.service;

import com.scalebiometrics.api.dto.AsyncMatchResponseDto;
import com.scalebiometrics.api.dto.MatchRequestDto;
import com.scalebiometrics.api.dto.VerificationRequestDto;
import com.scalebiometrics.api.entity.MatchRequest;
import com.scalebiometrics.api.entity.MatchRequestFingerprint;
import com.scalebiometrics.api.repository.MatchRequestFingerprintRepository;
import com.scalebiometrics.api.repository.MatchRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncMatchingService {

    private final MatchRequestRepository matchRequestRepository;
    private final MatchRequestFingerprintRepository matchRequestFingerprintRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MinioService minioService;
    private final BiometricService biometricService;

    private static final String TOPIC_MATCHING_REQUESTS = "matching-requests";

    @Transactional
    public AsyncMatchResponseDto submitMatch1N(MatchRequestDto requestDto, List<MultipartFile> fingerprints, String tenantId) {
        String traceId = UUID.randomUUID().toString();
        
        // 1. Persist request
        MatchRequest matchRequest = MatchRequest.builder()
                .tenantId(tenantId)
                .traceId(traceId)
                .status(MatchRequest.RequestStatus.PENDING)
                .requestType(MatchRequest.RequestType.MATCH_1_N)
                .probeRid(requestDto.getProbeRid())
                .build();
        
        matchRequest = matchRequestRepository.save(matchRequest);
        
        List<byte[]> templates = new ArrayList<>();

        // 2. Process each fingerprint
        for (int i = 0; i < fingerprints.size(); i++) {
            MultipartFile file = fingerprints.get(i);
            try {
                // Upload to MinIO
                String imagePath = minioService.uploadFile(tenantId, requestDto.getProbeRid(), file, i);
                
                // Extract Template
                byte[] imageBytes = file.getBytes();
                byte[] template = biometricService.extractTemplate(imageBytes);
                templates.add(template);

                // Save Fingerprint Metadata
                MatchRequestFingerprint fingerprint = MatchRequestFingerprint.builder()
                        .matchRequest(matchRequest)
                        .imagePath(imagePath)
                        .templateData(template)
                        .fingerIndex(i)
                        .build();
                
                matchRequestFingerprintRepository.save(fingerprint);

            } catch (IOException e) {
                log.error("Error processing file for request {}", matchRequest.getId(), e);
                throw new RuntimeException("Error processing fingerprint file", e);
            }
        }
        
        // 3. Publish to Kafka
        AsyncMatchRequestEvent event = AsyncMatchRequestEvent.builder()
                .requestId(matchRequest.getId())
                .tenantId(tenantId)
                .traceId(traceId)
                .type("MATCH_1_N")
                .probeRid(requestDto.getProbeRid())
                .templates(templates)
                .topK(requestDto.getTopK())
                .build();
                
        kafkaTemplate.send(TOPIC_MATCHING_REQUESTS, tenantId, event);
        
        return AsyncMatchResponseDto.builder()
                .requestId(matchRequest.getId())
                .status("ACCEPTED")
                .message("Request submitted for processing")
                .traceId(traceId)
                .build();
    }

    @Transactional
    public AsyncMatchResponseDto submitMatch1To1(VerificationRequestDto requestDto, String tenantId) {
        // TODO: Implement 1:1 with multipart if needed, for now keeping simple
        String traceId = UUID.randomUUID().toString();
        
        MatchRequest matchRequest = MatchRequest.builder()
                .tenantId(tenantId)
                .traceId(traceId)
                .status(MatchRequest.RequestStatus.PENDING)
                .requestType(MatchRequest.RequestType.MATCH_1_1)
                .probeRid(requestDto.getProbeRid())
                .targetRid(requestDto.getTargetRid())
                .build();
        
        matchRequest = matchRequestRepository.save(matchRequest);
        
        AsyncMatchRequestEvent event = AsyncMatchRequestEvent.builder()
                .requestId(matchRequest.getId())
                .tenantId(tenantId)
                .traceId(traceId)
                .type("MATCH_1_1")
                .payload(requestDto)
                .build();
                
        kafkaTemplate.send(TOPIC_MATCHING_REQUESTS, tenantId, event);
        
        return AsyncMatchResponseDto.builder()
                .requestId(matchRequest.getId())
                .status("ACCEPTED")
                .message("Request submitted for processing")
                .traceId(traceId)
                .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class AsyncMatchRequestEvent {
        private UUID requestId;
        private String tenantId;
        private String traceId;
        private String type;
        private String probeRid;
        private List<byte[]> templates;
        private int topK;
        private Object payload; // For backward compatibility or 1:1
    }
}
