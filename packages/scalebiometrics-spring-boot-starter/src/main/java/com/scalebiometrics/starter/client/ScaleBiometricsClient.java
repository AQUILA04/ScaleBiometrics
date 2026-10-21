package com.scalebiometrics.starter.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalebiometrics.starter.config.ScaleBiometricsProperties;
import com.scalebiometrics.starter.dto.AsyncMatchResponse;
import com.scalebiometrics.starter.dto.MatchRequest;
import com.scalebiometrics.starter.dto.VerificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ScaleBiometricsClient {

    private final RestTemplate restTemplate;
    private final ScaleBiometricsProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Submit an asynchronous 1:N matching request using raw byte arrays.
     * 
     * @param request The match request metadata
     * @param fingerprintImages List of raw fingerprint images (bytes)
     * @return The async response containing the request ID
     */
    public AsyncMatchResponse submitMatch1N(MatchRequest request, List<byte[]> fingerprintImages) {
        List<Resource> resources = new ArrayList<>();
        for (int i = 0; i < fingerprintImages.size(); i++) {
            byte[] imageBytes = fingerprintImages.get(i);
            final String filename = "fingerprint_" + i + ".png";
            resources.add(new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });
        }
        return doSubmitMatch1N(request, resources);
    }

    /**
     * Submit an asynchronous 1:N matching request using Spring MultipartFiles.
     * 
     * @param request The match request metadata
     * @param fingerprintFiles List of Spring MultipartFiles
     * @return The async response containing the request ID
     */
    public AsyncMatchResponse submitMatch1NMultipart(MatchRequest request, List<MultipartFile> fingerprintFiles) {
        List<Resource> resources = new ArrayList<>();
        for (MultipartFile file : fingerprintFiles) {
            resources.add(file.getResource());
        }
        return doSubmitMatch1N(request, resources);
    }

    private AsyncMatchResponse doSubmitMatch1N(MatchRequest request, List<Resource> resources) {
        String url = properties.getApiUrl() + "/api/matching/async/1n";
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Add JSON request metadata
            HttpHeaders jsonHeaders = new HttpHeaders();
            jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> jsonEntity = new HttpEntity<>(objectMapper.writeValueAsString(request), jsonHeaders);
            body.add("request", jsonEntity);
            
            // Add fingerprint files
            for (Resource resource : resources) {
                HttpHeaders fileHeaders = new HttpHeaders();
                // We use application/octet-stream as fallback, though the server usually relies on file extension or magic bytes
                fileHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                HttpEntity<Resource> fileEntity = new HttpEntity<>(resource, fileHeaders);
                body.add("fingerprints", fileEntity);
            }
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            log.debug("Sending match 1:N request to {} for probeRid: {}", url, request.getProbeRid());
            ResponseEntity<AsyncMatchResponse> response = restTemplate.postForEntity(url, requestEntity, AsyncMatchResponse.class);
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("Error submitting match 1:N request to ScaleBiometrics API", e);
            throw new RuntimeException("Failed to submit match request to ScaleBiometrics", e);
        }
    }

    /**
     * Submit an asynchronous 1:1 verification request.
     * 
     * @param request The verification request metadata
     * @return The async response containing the request ID
     */
    public AsyncMatchResponse submitMatch1To1(VerificationRequest request) {
        String url = properties.getApiUrl() + "/api/matching/async/1to1";
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<VerificationRequest> requestEntity = new HttpEntity<>(request, headers);
            
            log.debug("Sending match 1:1 request to {} for probeRid: {} vs targetRid: {}", 
                    url, request.getProbeRid(), request.getTargetRid());
            
            ResponseEntity<AsyncMatchResponse> response = restTemplate.postForEntity(url, requestEntity, AsyncMatchResponse.class);
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("Error submitting match 1:1 request to ScaleBiometrics API", e);
            throw new RuntimeException("Failed to submit verification request to ScaleBiometrics", e);
        }
    }
}
