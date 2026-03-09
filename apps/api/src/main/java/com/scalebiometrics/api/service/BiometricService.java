package com.scalebiometrics.api.service;

import com.machinezoo.sourceafis.FingerprintImage;
import com.machinezoo.sourceafis.FingerprintImageOptions;
import com.machinezoo.sourceafis.FingerprintTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BiometricService {

    public byte[] extractTemplate(byte[] imageBytes) {
        try {
            FingerprintTemplate template = new FingerprintTemplate(
                    new FingerprintImage()
                            .decode(imageBytes)
            );
            return template.toByteArray();
        } catch (Exception e) {
            log.error("Error extracting fingerprint template", e);
            throw new RuntimeException("Failed to extract biometric template", e);
        }
    }
}
