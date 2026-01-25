# Backend Build Pipeline

## Overview

The GitHub Actions backend CI pipeline has been optimized to handle the monorepo structure with shared library dependencies. The pipeline ensures that `biometric-core` is built and installed to the local Maven repository before other modules that depend on it.

## Pipeline Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Backend CI Pipeline                      │
└─────────────────────────────────────────────────────────────┘
                            │
                ┌───────────┴───────────┐
                │                       │
        ┌───────▼────────┐     ┌────────▼────────┐
        │   Push Event   │     │  Pull Request   │
        └────────────────┘     └─────────────────┘
                │                       │
                └───────────┬───────────┘
                            │
                ┌───────────▼───────────┐
                │  Trigger Conditions   │
                │  - Path filters       │
                │  - Branch filters     │
                └───────────┬───────────┘
                            │
        ┌───────────────────┴───────────────────┐
        │                                       │
    ┌───▼──────────────────┐      ┌────────────▼─────────┐
    │  Build and Test Job  │      │  Security Scan Job   │
    │  (runs-on: ubuntu)   │      │  (depends on build)  │
    └───┬──────────────────┘      └──────────────────────┘
        │
        ├─ Step 1: Checkout code
        │
        ├─ Step 2: Setup JDK 21
        │
        ├─ Step 3: Build biometric-core
        │  └─ mvn clean install (local repo)
        │
        ├─ Step 4: Build worker module
        │  └─ mvn clean install (local repo)
        │
        ├─ Step 5: Build master module
        │  └─ mvn clean install (local repo)
        │
        ├─ Step 6: Build API module
        │  └─ mvn clean compile (uses all deps)
        │
        ├─ Step 7: Run unit tests
        │  └─ mvn test
        │
        ├─ Step 8: Run integration tests
        │  └─ mvn verify
        │
        ├─ Step 9: Generate coverage reports
        │  └─ mvn jacoco:report
        │
        ├─ Step 10: Upload to Codecov
        │
        ├─ Step 11: Archive test results
        │
        ├─ Step 12: Archive build artifacts
        │
        └─ Step 13: Build summary
```

## Build Order

The pipeline builds modules in dependency order:

### 1. **biometric-core** (Shared Library)
```bash
mvn clean install -B -DskipTests
```
- **Purpose**: Provides shared domain models and exceptions
- **Output**: `com.scalebiometrics:biometric-core:jar:0.1.0-SNAPSHOT`
- **Installed to**: Local Maven repository (`~/.m2/repository`)
- **Dependents**: api, worker, master

### 2. **worker** (Matching Engine Service)
```bash
mvn clean install -B -DskipTests
```
- **Purpose**: Builds the Worker service
- **Dependencies**: biometric-core
- **Output**: `com.scalebiometrics:worker:jar:0.1.0-SNAPSHOT`
- **Installed to**: Local Maven repository

### 3. **master** (Orchestrator Service)
```bash
mvn clean install -B -DskipTests
```
- **Purpose**: Builds the Master orchestrator service
- **Dependencies**: biometric-core
- **Output**: `com.scalebiometrics:master:jar:0.1.0-SNAPSHOT`
- **Installed to**: Local Maven repository

### 4. **api** (REST API Service)
```bash
mvn clean compile -B
```
- **Purpose**: Builds the API service
- **Dependencies**: biometric-core, worker, master
- **Output**: Compiled classes in `target/`

### 5. **All Modules** (Testing & Coverage)
```bash
mvn test -B           # Unit tests
mvn verify -B         # Integration tests
mvn jacoco:report -B  # Coverage reports
```

## Key Features

### ✅ Dependency Resolution
- **Local Repository Caching**: Maven cache is enabled via `cache: 'maven'`
- **Sequential Building**: Modules are built in dependency order
- **Install to Local Repo**: Each module is installed to `~/.m2/repository`

### ✅ Multi-Module Support
- **biometric-core**: Shared library
- **api**: REST API service
- **worker**: Matching engine service
- **master**: Orchestrator service

### ✅ Testing & Quality
- **Unit Tests**: `mvn test`
- **Integration Tests**: `mvn verify`
- **Code Coverage**: JaCoCo reports for all modules
- **Security Scanning**: Trivy vulnerability scanner

### ✅ Artifacts & Reports
- **Test Results**: Archived from all modules
- **Build Artifacts**: JAR files from all modules
- **Coverage Reports**: Uploaded to Codecov

## Path Filters

The pipeline is triggered when changes are made to:

```yaml
paths:
  - 'apps/api/**'                      # API service
  - 'apps/worker/**'                   # Worker service
  - 'apps/master/**'                   # Master service
  - 'packages/biometric-core/**'       # Shared library
  - 'pom.xml'                          # Parent POM
  - '.github/workflows/backend-ci.yml' # This workflow
```

## Branch Filters

The pipeline runs on:

**Push Events:**
- `main`
- `develop`
- `feature/**`
- `release/**`

**Pull Request Events:**
- `main`
- `develop`

## Services

The pipeline includes Docker services for testing:

### PostgreSQL 16
- **Port**: 5432
- **Database**: scalebiometrics_test
- **User**: test
- **Password**: test

### Redis 7
- **Port**: 6379
- **Health Check**: `redis-cli ping`

## Environment Variables

For integration tests:

```yaml
SPRING_PROFILES_ACTIVE: test
DB_HOST: localhost
DB_PORT: 5432
DB_NAME: scalebiometrics_test
DB_USER: test
DB_PASSWORD: test
REDIS_HOST: localhost
REDIS_PORT: 6379
```

## Troubleshooting

### Issue: "Could not find artifact com.scalebiometrics:biometric-core"

**Solution**: Ensure Step 3 (Build biometric-core) completes successfully before other modules build.

**Check**:
```bash
# Verify biometric-core is in local repo
ls -la ~/.m2/repository/com/scalebiometrics/biometric-core/
```

### Issue: "Maven cache not working"

**Solution**: The workflow uses `cache: 'maven'` which caches:
- `~/.m2/repository`
- `~/.m2/settings.xml`

If cache is not working, clear it and re-run.

### Issue: "Tests failing in CI but passing locally"

**Possible causes**:
- Different Java version (CI uses Java 21)
- Database/Redis not ready
- Environment variables not set

**Solution**: Check the service health checks and environment variables.

## Performance Optimization

### Maven Caching
- **Enabled**: `cache: 'maven'`
- **Saves**: ~2-3 minutes per build
- **Scope**: Entire workflow

### Parallel Testing
- **Unit Tests**: Run in parallel via Maven Surefire
- **Integration Tests**: Run sequentially (due to shared services)

### Skip Tests During Install
- **biometric-core install**: `-DskipTests` (tests run later)
- **worker install**: `-DskipTests` (tests run later)
- **master install**: `-DskipTests` (tests run later)
- **api compile**: No tests (only compile)

## Local Development

To replicate the CI build locally:

```bash
# Build in order
cd packages/biometric-core
mvn clean install

cd ../../apps/worker
mvn clean install

cd ../master
mvn clean install

cd ../api
mvn clean compile

# Run tests
cd ../..
mvn test
mvn verify
mvn jacoco:report
```

## Next Steps

- **Docker Build**: Push images to registry after successful build
- **Deployment**: Deploy to staging/production
- **Performance Testing**: Run load tests on staging
- **Security Review**: Review Trivy scan results

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Maven Documentation](https://maven.apache.org/)
- [JaCoCo Coverage](https://www.jacoco.org/)
- [Codecov Integration](https://codecov.io/)
- [Trivy Scanner](https://github.com/aquasecurity/trivy)
