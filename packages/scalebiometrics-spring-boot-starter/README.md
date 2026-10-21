# ScaleBiometrics Spring Boot Starter

This starter provides a seamless integration with the ScaleBiometrics API for Spring Boot applications. It handles OAuth2 authentication, provides a typed client for matching requests, and automatically configures a webhook endpoint to receive asynchronous matching results.

## Features

- **Auto-Configuration**: Automatically configures the `ScaleBiometricsClient` and OAuth2 authentication.
- **Typed Client**: `ScaleBiometricsClient` provides methods for interacting with the ScaleBiometrics API (async 1:N matching and 1:1 verification).
- **Multipart Support**: Accepts raw byte arrays or Spring `MultipartFile`s directly.
- **Webhook Integration**: Automatically exposes a webhook endpoint (`/api/webhooks/scalebiometrics`) to receive asynchronous match results.
- **Extensible Webhook Handling**: Easily override the default webhook processing logic by defining your own `ScaleBiometricsWebhookHandler` bean.

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.scalebiometrics</groupId>
    <artifactId>scalebiometrics-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Configuration

Configure the required properties in your `application.yml` or `application.properties`:

```yaml
scalebiometrics:
  api-url: "https://api.scalebiometrics.com"
  tenant-id: "your-tenant-id"
  client-id: "your-client-id"
  client-secret: "your-client-secret"
  token-uri: "https://auth.scalebiometrics.com/realms/scalebiometrics/protocol/openid-connect/token"
  webhook:
    enabled: true # true by default
    path: "/api/webhooks/scalebiometrics" # default path
```

## Usage

### 1. Submitting a 1:N Matching Request (Deduplication/Identification)

Inject the `ScaleBiometricsClient` into your service and use it to submit matching requests. You can pass raw bytes or Spring `MultipartFile`s.

```java
import com.scalebiometrics.starter.client.ScaleBiometricsClient;
import com.scalebiometrics.starter.dto.MatchRequest;
import com.scalebiometrics.starter.dto.AsyncMatchResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
public class MyBiometricService {

    private final ScaleBiometricsClient scaleBiometricsClient;

    public MyBiometricService(ScaleBiometricsClient scaleBiometricsClient) {
        this.scaleBiometricsClient = scaleBiometricsClient;
    }

    // Using raw bytes
    public void deduplicateUser(String userId, List<byte[]> fingerprintImages) {
        MatchRequest request = MatchRequest.builder()
                .probeRid(userId)
                .topK(1)
                .threshold(40.0f)
                .build();

        AsyncMatchResponse response = scaleBiometricsClient.submitMatch1N(request, fingerprintImages);
        System.out.println("Request submitted. ID: " + response.getRequestId());
    }

    // Using Spring MultipartFile (e.g., directly from a REST controller)
    public void deduplicateUserMultipart(String userId, List<MultipartFile> fingerprintFiles) {
        MatchRequest request = MatchRequest.builder()
                .probeRid(userId)
                .topK(1)
                .threshold(40.0f)
                .build();

        AsyncMatchResponse response = scaleBiometricsClient.submitMatch1NMultipart(request, fingerprintFiles);
        System.out.println("Request submitted. ID: " + response.getRequestId());
    }
}
```

### 2. Submitting a 1:1 Verification Request (Authentication)

```java
import com.scalebiometrics.starter.client.ScaleBiometricsClient;
import com.scalebiometrics.starter.dto.VerificationRequest;
import com.scalebiometrics.starter.dto.AsyncMatchResponse;
import org.springframework.stereotype.Service;

@Service
public class MyVerificationService {

    private final ScaleBiometricsClient scaleBiometricsClient;

    public MyVerificationService(ScaleBiometricsClient scaleBiometricsClient) {
        this.scaleBiometricsClient = scaleBiometricsClient;
    }

    public void verifyUser(String probeId, byte[] probeTemplate, String targetId) {
        VerificationRequest request = VerificationRequest.builder()
                .probeRid(probeId)
                .probeTemplate(probeTemplate)
                .targetRid(targetId)
                .build();

        AsyncMatchResponse response = scaleBiometricsClient.submitMatch1To1(request);
        System.out.println("Verification submitted. ID: " + response.getRequestId());
    }
}
```

### 3. Handling Webhook Results

By default, the starter will log incoming webhook results. To implement your own custom logic (e.g., updating your database), simply define a Spring Bean that implements `ScaleBiometricsWebhookHandler`:

```java
import com.scalebiometrics.starter.dto.MatchResultEvent;
import com.scalebiometrics.starter.webhook.ScaleBiometricsWebhookHandler;
import org.springframework.stereotype.Component;

@Component
public class MyCustomWebhookHandler implements ScaleBiometricsWebhookHandler {

    @Override
    public void handleMatchResult(MatchResultEvent result) {
        System.out.println("Received match result for Trace ID: " + result.getTraceId());
        
        if ("COMPLETED".equals(result.getStatus()) && !result.getCandidates().isEmpty()) {
            MatchResultEvent.Candidate topMatch = result.getCandidates().get(0);
            if (topMatch.isMatch()) {
                System.out.println("Duplicate found! Matched with: " + topMatch.getTargetRid());
                // TODO: Update your local database or notify the user
            }
        }
    }
}
```
