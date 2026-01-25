package com.scalebiometrics.worker.engine;

import com.scalebiometrics.core.exception.BiometricException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SourceAFIS Matcher - Wraps SourceAFIS library for exact fingerprint matching.
 * 
 * SourceAFIS is a high-accuracy fingerprint matching algorithm that:
 * - Performs minutiae-based matching
 * - Achieves high accuracy with low false positive rate
 * - Provides similarity scores (0-100)
 * 
 * Responsibilities:
 * - Perform exact matching between two fingerprints
 * - Provide similarity scores
 * - Handle template validation
 */
@Slf4j
@Component
public class SourceAFISMatcher {

    private static final int MIN_TEMPLATE_SIZE = 32;
    private static final int MAX_TEMPLATE_SIZE = 4096;

    /**
     * Perform exact matching between two fingerprints
     * 
     * @param probeTemplate Binary template of probe fingerprint
     * @param targetTemplate Binary template of target fingerprint
     * @return Similarity score (0-100)
     */
    public int match(byte[] probeTemplate, byte[] targetTemplate) throws BiometricException {
        if (probeTemplate == null || targetTemplate == null) {
            throw new BiometricException("Template cannot be null");
        }

        if (probeTemplate.length < MIN_TEMPLATE_SIZE || probeTemplate.length > MAX_TEMPLATE_SIZE) {
            throw new BiometricException("Invalid probe template size: " + probeTemplate.length);
        }

        if (targetTemplate.length < MIN_TEMPLATE_SIZE || targetTemplate.length > MAX_TEMPLATE_SIZE) {
            throw new BiometricException("Invalid target template size: " + targetTemplate.length);
        }

        try {
            // Call SourceAFIS matching algorithm
            // This is a placeholder - actual implementation would use SourceAFIS library
            int score = performMatching(probeTemplate, targetTemplate);

            log.debug("SourceAFIS matching completed - Score: {}", score);
            return score;

        } catch (Exception e) {
            log.error("Error performing SourceAFIS matching", e);
            throw new BiometricException("SourceAFIS matching failed: " + e.getMessage(), e);
        }
    }

    /**
     * Perform the actual matching (placeholder implementation)
     * 
     * In production, this would call the actual SourceAFIS library
     */
    private int performMatching(byte[] probeTemplate, byte[] targetTemplate) {
        // Placeholder: Calculate a simple similarity score based on template comparison
        // In production, this would use SourceAFIS algorithm
        
        int minLength = Math.min(probeTemplate.length, targetTemplate.length);
        int matchingBytes = 0;

        for (int i = 0; i < minLength; i++) {
            if (probeTemplate[i] == targetTemplate[i]) {
                matchingBytes++;
            }
        }

        // Convert to 0-100 scale
        return (matchingBytes * 100) / minLength;
    }

    /**
     * Validate a template
     */
    public boolean validateTemplate(byte[] template) {
        if (template == null) {
            return false;
        }

        if (template.length < MIN_TEMPLATE_SIZE || template.length > MAX_TEMPLATE_SIZE) {
            return false;
        }

        // Check for minimum entropy (not all zeros or all ones)
        int zeroCount = 0;
        int oneCount = 0;

        for (byte b : template) {
            if (b == 0) {
                zeroCount++;
            } else if (b == (byte) 0xFF) {
                oneCount++;
            }
        }

        // If more than 80% of bytes are the same, template is likely invalid
        int sameCount = Math.max(zeroCount, oneCount);
        return sameCount < (template.length * 0.8);
    }

    /**
     * Get matcher version
     */
    public String getVersion() {
        return "SourceAFIS-3.18.1";
    }
}
