package com.scalebiometrics.core.exception;

/**
 * Base exception for all biometric processing errors.
 */
public class BiometricException extends RuntimeException {
    private final String errorCode;

    public BiometricException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BiometricException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
