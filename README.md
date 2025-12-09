# ScaleBiometrics - National-Grade Biometric Deduplication SaaS

![CI/CD](https://github.com/AQUILA04/ScaleBiometrics/actions/workflows/backend-ci.yml/badge.svg) ![CI/CD](https://github.com/AQUILA04/ScaleBiometrics/actions/workflows/frontend-ci.yml/badge.svg) ![CI/CD](https://github.com/AQUILA04/ScaleBiometrics/actions/workflows/docker-build.yml/badge.svg)

**ScaleBiometrics** is a high-performance, distributed, and event-driven SaaS platform for biometric deduplication and identification. It is designed to handle massive concurrency and large-scale databases with a focus on performance, scalability, and strict multi-tenancy.

## 🚀 Features

- **High Performance:** 1:N deduplication matching in under 2 seconds for databases up to 10M records.
- **Scalable:** Distributed Master-Worker architecture with Java 21 Virtual Threads.
- **Multi-Tenant:** Strict data isolation with a Schema-per-tenant model.
- **Interoperable:** Standardized integration via REST, gRPC, and Kafka.
- **Secure:** Perimeter Security Model with Keycloak for IAM.
- **Observable:** Distributed tracing with OpenTelemetry and structured logging.

## 🛠️ Tech Stack

| Category | Technology | Version |
| :--- | :--- | :--- |
| **Language** | Java | 21 (LTS) |
| **Framework** | Spring Boot | 3.2+ |
| **Frontend** | Next.js | 14+ |
| **Matcher** | SourceAFIS | Latest |
| **Bus** | Kafka | 3.6+ |
| **RPC** | gRPC | 1.60+ |
| **DB** | PostgreSQL | 16+ |
| **Storage** | MinIO | Latest |
| **IAM** | Keycloak | 23+ |
| **Cache** | Redis | 7+ |

## 📂 Monorepo Structure

```
ScaleBiometrics/
├── apps/
│   ├── api/          # Backend (Spring Boot)
│   └── web/          # Frontend (Next.js)
├── packages/
│   └── shared/       # Shared libraries (e.g., DTOs)
├── infrastructure/
│   ├── local/        # Docker Compose for local dev
│   ├── keycloak/     # Keycloak theme and config
│   └── scripts/      # Utility scripts
├── contracts/        # API contracts (OpenAPI, Protobuf)
├── deploy/
│   ├── dev/          # Build scripts
│   └── prod/         # Kubernetes manifests (future)
├── docs/             # Documentation (PRD, architecture, etc.)
└── .github/          # GitHub Actions workflows
```

## 🏁 Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 21
- Node.js 20 & pnpm 8
- Maven

### 1. Start Infrastructure

```bash
cd infrastructure/local
docker-compose up -d
```

This will start:
- PostgreSQL (port 5432)
- Redis (port 6379)
- Kafka (port 9092)
- MinIO (ports 9000, 9001)
- Keycloak (port 8080)
- MailDev (ports 1080, 1025)

### 2. Run Backend API

```bash
cd apps/api
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8081/api`.

### 3. Run Frontend Web

```bash
cd apps/web
pnpm install
pnpm dev
```

The web app will be available at `http://localhost:3000`.

## 🐳 Build Docker Images

To build all Docker images locally, run:

```bash
# For Linux/Mac
./deploy/dev/build-images.sh

# For Windows
.\deploy\dev\build-images.bat
```

## 🔒 Security & IAM

- **Keycloak Admin:** `http://localhost:8080`
- **Realm:** `scalebiometrics`
- **Superadmin:** `superadmin` / `SuperAdmin@123` (temporary password)
- **API Client:** `scalebiometrics-api`
- **Web Client:** `scalebiometrics-web`

## 📄 API Contracts

API contracts are defined in the `contracts/` directory. Please refer to the `README.md` in that directory for more information on the versioning and update process.

## 🧪 Testing

- **Backend:** `./mvnw test` (unit tests), `./mvnw verify` (integration tests)
- **Frontend:** `pnpm test`

## 🤝 Contributing

Please follow the Git Flow methodology. All changes must be introduced via pull requests to the `develop` branch.

## 📜 License

This project is licensed under the **Apache License 2.0**. See the `LICENSE` file for details.
