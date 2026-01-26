# Backend Build Pipeline

## Overview

The GitHub Actions backend CI pipeline has been optimized to handle the monorepo structure with shared library dependencies and parent POM management. The pipeline ensures that:

1. **Parent POM** is installed first (required by all modules)
2. **biometric-core** is built and installed (shared library)
3. **worker** and **master** are built (services)
4. **api** is built (depends on all above)
5. All tests and coverage reports are generated

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
        ├─ Step 3: Install parent POM
        │  └─ mvn install -B -DskipTests -N
        │     └─ Installs to local repo
        │
        ├─ Step 4: Build biometric-core
        │  └─ mvn clean install (local repo)
        │
        ├─ Step 5: Build worker module
        │  └─ mvn clean install (local repo)
        │
        ├─ Step 6: Build master module
        │  └─ mvn clean install (local repo)
        │
        ├─ Step 7: Build API module
        │  └─ mvn clean compile (uses all deps)
        │
        ├─ Step 8: Run unit tests
        │  └─ mvn test
        │
        ├─ Step 9: Run integration tests
        │  └─ mvn verify
        │
        ├─ Step 10: Generate coverage reports
        │  └─ mvn jacoco:report
        │
        ├─ Step 11: Upload to Codecov
        │
        ├─ Step 12: Archive test results
        │
        ├─ Step 13: Archive build artifacts
        │
        └─ Step 14: Build summary
```

## Build Order

The pipeline builds modules in strict dependency order to ensure all parent POMs and dependencies are available:

### 0. **Parent POM** (scalebiometrics-parent)
```bash
mvn install -B -DskipTests -N
```
- **Purpose**: Installs the parent POM to local Maven repository
- **Output**: `com.scalebiometrics:scalebiometrics-parent:pom:0.1.0-SNAPSHOT`
- **Installed to**: Local Maven repository (`~/.m2/repository`)
- **Required by**: All child modules (biometric-core, api, worker, master)
- **Flag `-N`**: Non-recursive (only install parent, not children)
- **Why first**: All modules declare `<parent>` reference to this POM

### 1. **biometric-core** (Shared Library)
```bash
mvn clean install -B -DskipTests
```
- **Purpose**: Provides shared domain models and exceptions
- **Dependencies**: parent POM
- **Output**: `com.scalebiometrics:biometric-core:jar:0.1.0-SNAPSHOT`
- **Installed to**: Local Maven repository (`~/.m2/repository`)
- **Dependents**: api, worker, master

### 2. **worker** (Matching Engine Service)
```bash
mvn clean install -B -DskipTests
```
- **Purpose**: Builds the Worker service
- **Dependencies**: parent POM, biometric-core
- **Output**: `com.scalebiometrics:worker:jar:0.1.0-SNAPSHOT`
- **Installed to**: Local Maven repository

### 3. **master** (Orchestrator Service)
```bash
mvn clean install -B -DskipTests
```
- **Purpose**: Builds the Master orchestrator service
- **Dependencies**: parent POM, biometric-core
- **Output**: `com.scalebiometrics:master:jar:0.1.0-SNAPSHOT`
- **Installed to**: Local Maven repository

### 4. **api** (REST API Service)
```bash
mvn clean compile -B
```
- **Purpose**: Builds the API service
- **Dependencies**: parent POM, biometric-core, worker, master
- **Output**: Compiled classes in `target/`

### 5. **All Modules** (Testing & Coverage)
```bash
mvn test -B           # Unit tests
mvn verify -B         # Integration tests
mvn jacoco:report -B  # Coverage reports
```

## Dependency Graph

```
scalebiometrics-parent (parent POM)
    │
    ├─ packages/biometric-core
    │   ├─ apps/api
    │   ├─ apps/worker
    │   └─ apps/master
    │
    ├─ apps/api
    │   └─ depends on: biometric-core, worker, master
    │
    ├─ apps/worker
    │   └─ depends on: biometric-core
    │
    └─ apps/master
        └─ depends on: biometric-core
```

## Key Features

### ✅ Dependency Resolution
- **Parent POM First**: Installed with `-N` flag before child modules
- **Local Repository Caching**: Maven cache is enabled via `cache: 'maven'`
- **Sequential Building**: Modules are built in dependency order
- **Install to Local Repo**: Each module is installed to `~/.m2/repository`

### ✅ Multi-Module Support
- **scalebiometrics-parent**: Parent POM
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

### Issue: "Could not find artifact com.scalebiometrics:scalebiometrics-parent:pom"

**Root Cause**: Parent POM was not installed before child modules.

**Solution**: Ensure Step 3 (Install parent POM) completes successfully with `-N` flag.

**Check**:
```bash
# Verify parent POM is in local repo
ls -la ~/.m2/repository/com/scalebiometrics/scalebiometrics-parent/
```

**Debug**:
```bash
# Run parent install manually
mvn install -B -DskipTests -N
```

### Issue: "Could not find artifact com.scalebiometrics:biometric-core"

**Root Cause**: biometric-core was not built before other modules.

**Solution**: Ensure Step 4 (Build biometric-core) completes successfully before other modules.

**Check**:
```bash
# Verify biometric-core is in local repo
ls -la ~/.m2/repository/com/scalebiometrics/biometric-core/
```

**Debug**:
```bash
# Build biometric-core manually
cd packages/biometric-core
mvn clean install -B -DskipTests
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
- Parent POM not installed

**Solution**: 
1. Check the service health checks
2. Verify environment variables are set
3. Ensure parent POM is installed first
4. Run locally with same Java version

## Performance Optimization

### Maven Caching
- **Enabled**: `cache: 'maven'`
- **Saves**: ~2-3 minutes per build
- **Scope**: Entire workflow

### Parallel Testing
- **Unit Tests**: Run in parallel via Maven Surefire
- **Integration Tests**: Run sequentially (due to shared services)

### Skip Tests During Install
- **Parent POM install**: `-DskipTests` (no tests for parent)
- **biometric-core install**: `-DskipTests` (tests run later)
- **worker install**: `-DskipTests` (tests run later)
- **master install**: `-DskipTests` (tests run later)
- **api compile**: No tests (only compile)

## Local Development

To replicate the CI build locally:

```bash
# Step 0: Install parent POM first (CRITICAL!)
mvn install -B -DskipTests -N

# Step 1: Build biometric-core
cd packages/biometric-core
mvn clean install

# Step 2: Build worker
cd ../../apps/worker
mvn clean install

# Step 3: Build master
cd ../master
mvn clean install

# Step 4: Build API
cd ../api
mvn clean compile

# Step 5: Run tests
cd ../..
mvn test
mvn verify
mvn jacoco:report
```

**Important**: Always install the parent POM first with `-N` flag to avoid building child modules.

## Verifying Local Setup

```bash
# Check parent POM is installed
mvn help:describe -Ddetail=true

# Check biometric-core is available
mvn dependency:tree | grep biometric-core

# Check all modules compile
mvn clean compile

# Run all tests
mvn test

# Generate coverage
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
- [Maven Multi-Module Projects](https://maven.apache.org/guides/mini/guide-multiple-modules.html)
- [JaCoCo Coverage](https://www.jacoco.org/)
- [Codecov Integration](https://codecov.io/)
- [Trivy Scanner](https://github.com/aquasecurity/trivy)
