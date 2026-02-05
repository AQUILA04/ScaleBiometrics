# API Implementation Plan

**Status:** Draft
**Target Module:** `apps/api`
**Date:** 2025-12-25

## Overview
The `apps/api` module acts as the API Gateway and Ingestion Service for the ScaleBiometrics platform. It is responsible for handling external REST requests, authentication, validation, and routing commands to the Master Node (via gRPC) or processing data ingestion (MinIO/Kafka/DB).

Currently, the module is a skeleton. This document outlines the steps to implement the required functionality.

## 1. Project Scaffolding
Create the following package structure under `com.scalebiometrics.api`:
- `controller`: REST API endpoints.
- `service`: Business logic and external service integration.
- `repository`: Spring Data JPA repositories.
- `dto`: Data Transfer Objects for API requests/responses.
- `config`: Configuration classes (Security, MinIO, gRPC, Swagger).
- `exception`: Global exception handling.
- `mapper`: Object mapping (DTO <-> Domain/Proto).

## 2. Data Transfer Objects (DTOs)
Define DTOs to decouple the API from internal domain models and Protobuf classes.
- **Matching**:
  - `MatchRequestDto` (probeRid, topK, threshold, etc.)
  - `MatchResponseDto` (status, candidates, latency)
  - `VerificationRequestDto`
  - `VerificationResponseDto`
- **Fingerprint**:
  - `FingerprintUploadRequest` (multipart/form-data)
  - `FingerprintResponseDto`

## 3. gRPC Client Implementation
Implement the communication layer with the Master Node.
- **Service**: `MatchingService`
- **Logic**:
  - Initialize `MatcherServiceGrpc.MatcherServiceBlockingStub`.
  - Convert `MatchRequestDto` to `MatcherProto.MatchRequest`.
  - Call `Match1N` or `Match1To1`.
  - Convert `MatcherProto.MatchResponse` back to `MatchResponseDto`.
  - Handle gRPC exceptions (TIMEOUT, UNAVAILABLE).

## 4. Fingerprint Ingestion Service
Implement the logic for processing new fingerprints.
- **Service**: `FingerprintService`
- **Dependencies**: `MinioClient`, `FingerprintRepository`, `KafkaTemplate`.
- **Flow**:
  1. Validate input (format, quality).
  2. Upload raw image to MinIO bucket (`biometric-images`).
  3. Save metadata to PostgreSQL (`t_fingerprints` table).
  4. Publish `FingerprintIngestedEvent` to Kafka topic (`fingerprint-ingestion`).

## 5. REST Controllers
Implement the endpoints defined in the Architecture guide.

### MatchingController
- `POST /api/v1/matching/1n`: Delegates to `MatchingService.match1N`.
- `POST /api/v1/matching/1to1`: Delegates to `MatchingService.match1To1`.

### FingerprintController
- `POST /api/v1/fingerprints`: Handles `MultipartFile` upload. Delegates to `FingerprintService`.
- `GET /api/v1/fingerprints/{id}`: Retrieves metadata.
- `DELETE /api/v1/fingerprints/{id}`: Deletes data.

### HealthController
- `GET /api/v1/health`: Performs deep health check (DB, Redis, Kafka, Master connectivity).

## 6. Infrastructure & Configuration
- **Security**: Implement `SecurityConfig` to enable OAuth2 Resource Server (JWT validation).
- **MinIO**: Configure `MinioClient` bean.
- **Swagger/OpenAPI**: Add `springdoc-openapi` for API documentation.
- **Global Exception Handler**: `RestExceptionHandler` to translate exceptions to standard JSON error responses.

## 7. Verification
- Unit tests for Services and Controllers.
- Integration tests using Testcontainers (PostgreSQL, Kafka, MinIO).
