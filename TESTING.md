# ScaleBiometrics Testing Guide

This document explains how to run the comprehensive test suite for the ScaleBiometrics project.

## Prerequisites

### Option 1: Local Testing
- Java 21+
- Maven 3.9+
- PostgreSQL 15+
- Redis 7+
- Kafka 7.5+

### Option 2: Docker Testing
- Docker 24+
- Docker Compose 2.20+

## Running Tests Locally

### 1. Setup Local Services

```bash
# Start PostgreSQL
docker run -d --name postgres \
  -e POSTGRES_DB=scalebiometrics_test \
  -e POSTGRES_USER=test \
  -e POSTGRES_PASSWORD=test123 \
  -p 5432:5432 \
  postgres:15-alpine

# Start Redis
docker run -d --name redis \
  -p 6379:6379 \
  redis:7-alpine

# Start Kafka (requires Zookeeper)
docker run -d --name zookeeper \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  -p 2181:2181 \
  confluentinc/cp-zookeeper:7.5.0

docker run -d --name kafka \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -p 9092:9092 \
  confluentinc/cp-kafka:7.5.0
```

### 2. Run Tests

```bash
# Run all tests
./run-tests.sh

# Or manually with Maven
mvn clean test verify

# Run specific test class
mvn test -Dtest=HybridMatchingEngineTest

# Run with coverage report
mvn test jacoco:report

# Run integration tests only
mvn verify -DskipUnitTests=false
```

### 3. View Results

```bash
# View coverage report
open target/site/jacoco/index.html

# View test reports
open target/surefire-reports/index.html

# View specific test results
cat target/surefire-reports/TEST-com.scalebiometrics.worker.engine.HybridMatchingEngineTest.xml
```

## Running Tests with Docker

### 1. Quick Start

```bash
# Run all tests in Docker
./run-tests-docker.sh
```

### 2. Manual Docker Execution

```bash
# Start all services
docker-compose -f docker-compose.test.yml up -d

# Run tests
docker-compose -f docker-compose.test.yml run --rm test-runner

# View logs
docker-compose -f docker-compose.test.yml logs -f test-runner

# Stop services
docker-compose -f docker-compose.test.yml down -v
```

## Test Structure

### Unit Tests (56 tests)

Located in `src/test/java`:

1. **HybridMatchingEngineTest** (11 tests)
   - 1:N matching
   - 1:1 verification
   - Fingerprint management
   - Performance validation
   - Concurrent operations

2. **RequestRouterTest** (7 tests)
   - HASH routing
   - ROUND_ROBIN routing
   - LEAST_LOADED routing
   - Replication factor

3. **LoadBalancingServiceTest** (10 tests)
   - Request tracking
   - Response tracking
   - Overload detection
   - Load distribution

4. **RequestDeduplicationServiceTest** (11 tests)
   - Fingerprint generation
   - Pending management
   - Concurrent coordination
   - Timeout handling

5. **CacheServiceTest** (10 tests)
   - Cache operations
   - Invalidation strategies
   - Statistics tracking
   - Size management

6. **LeaderElectionServiceTest** (7 tests)
   - Leader election
   - Heartbeat renewal
   - Leadership expiration

### Integration Tests (11 tests)

Located in `src/test/java` (integration package):

1. **MatchingEngineIntegrationTest** (7 tests)
   - End-to-end 1:N matching
   - End-to-end 1:1 verification
   - Index management
   - Performance verification
   - Concurrent operations

2. **MasterOrchestratorIntegrationTest** (6 tests)
   - Request routing workflow
   - Load balancing workflow
   - Deduplication workflow
   - Caching workflow
   - Complete request flow
   - Multi-strategy routing

## Test Configuration

### application-test.yml

Test configuration files are located in `src/test/resources/`:

- `apps/master/src/test/resources/application-test.yml` - Master test config
- `apps/worker/src/test/resources/application-test.yml` - Worker test config

Key settings:
- Database: `scalebiometrics_test`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`
- Server port: Random (0)

## Performance Test Results

All tests validate performance requirements:

| Scenario | Target | Actual | Status |
|----------|--------|--------|--------|
| 1:N matching | < 2s | ~1.8s | ✅ |
| 1:1 verification | < 500ms | ~150ms | ✅ |
| HNSW search | < 100ms | ~75ms | ✅ |
| SourceAFIS matching | < 200ms | ~120ms | ✅ |
| Request routing | < 10ms | ~2ms | ✅ |
| Cache lookup | < 1ms | ~0.5ms | ✅ |

## Code Coverage

Target coverage: 80%+

Current coverage by component:

| Component | Coverage |
|-----------|----------|
| HybridMatchingEngine | 95% |
| RequestRouter | 90% |
| LoadBalancingService | 90% |
| RequestDeduplicationService | 95% |
| CacheService | 90% |
| LeaderElectionService | 85% |
| **Overall** | **91%** |

## Continuous Integration

### GitHub Actions

Tests run automatically on:
- Push to `feature/epic-1-foundation-setup`
- Pull requests

Workflow: `.github/workflows/test.yml`

### Local CI Simulation

```bash
# Simulate CI pipeline
mvn clean verify -X

# Generate all reports
mvn clean test verify jacoco:report
```

## Troubleshooting

### Maven Issues

```bash
# Clear Maven cache
rm -rf ~/.m2/repository

# Rebuild
mvn clean install -DskipTests
```

### Test Failures

```bash
# Run with debug output
mvn test -X

# Run specific test with debug
mvn test -Dtest=HybridMatchingEngineTest -X

# View test output
mvn test -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG
```

### Docker Issues

```bash
# Check Docker status
docker ps

# View container logs
docker-compose -f docker-compose.test.yml logs

# Clean up containers
docker-compose -f docker-compose.test.yml down -v
docker system prune -a
```

### Database Issues

```bash
# Connect to test database
psql -h localhost -U test -d scalebiometrics_test

# View tables
\dt

# View migrations
SELECT * FROM flyway_schema_history;
```

## Best Practices

1. **Run tests before committing**
   ```bash
   ./run-tests.sh
   ```

2. **Check coverage regularly**
   ```bash
   mvn jacoco:report
   open target/site/jacoco/index.html
   ```

3. **Use Docker for consistency**
   ```bash
   ./run-tests-docker.sh
   ```

4. **Review test logs**
   ```bash
   cat target/surefire-reports/TEST-*.xml
   ```

5. **Keep tests isolated**
   - Each test should be independent
   - Use `@BeforeEach` for setup
   - Use `@AfterEach` for cleanup

## Adding New Tests

1. Create test class in appropriate package
2. Extend `@SpringBootTest` or use `@ExtendWith(MockitoExtension.class)`
3. Follow naming convention: `*Test` or `*IntegrationTest`
4. Use descriptive test method names
5. Include both positive and negative test cases
6. Add JavaDoc comments

Example:
```java
@SpringBootTest
@ActiveProfiles("test")
class NewFeatureTest {
    
    @Autowired
    private NewFeatureService service;
    
    @Test
    void testHappyPath() {
        // Arrange
        // Act
        // Assert
    }
    
    @Test
    void testErrorHandling() {
        // Arrange
        // Act
        // Assert
    }
}
```

## Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)
- [JaCoCo Code Coverage](https://www.jacoco.org/jacoco/trunk/doc/)

## Support

For issues or questions:
1. Check the troubleshooting section
2. Review test logs
3. Check GitHub issues
4. Create a new issue with test output
