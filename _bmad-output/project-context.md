---
project_name: 'ScaleBiometrics'
user_name: 'Francis'
date: '2026-03-09T16:49:51+04:00'
sections_completed:
  ['technology_stack', 'language_rules', 'framework_rules', 'testing_rules', 'code_quality_rules', 'development_workflow_rules', 'critical_rules']
status: 'complete'
rule_count: 17
optimized_for_llm: true
---

# Project Context for AI Agents

_This file contains critical rules and patterns that AI agents must follow when implementing code in this project. Focus on unobvious details that agents might otherwise miss._

---

## Technology Stack & Versions

**Core Technologies:**
- Backend: Java 21 (with `--enable-preview` enabled), Spring Boot 3.2.0
- Frontend: Next.js 14.2.33 (App Router), React 18
- Build Tools: Maven 3.11 for Backend, npm/pnpm for Frontend

**Key Dependencies:**
- Messaging & RPC: gRPC 1.60.0, Protobuf 3.25.1, Kafka
- Database: PostgreSQL with Liquibase
- Biometrics & Search: SourceAFIS 3.18.1, JVector 3.0.6
- Object Storage: MinIO 8.5.7
- Observability: OpenTelemetry 1.32.0, Micrometer 1.12.0
- CSS Framework: Tailwind CSS 3.4.1

## Critical Implementation Rules

### Language-Specific Rules

**Java (Backend):**
- **Java 21 Preview**: You MUST use `--enable-preview` when compiling and running (Maven compiler/surefire argLine). Features like `MemorySegment` and `Arena` (Vector API) are actively used. 
- **Standard Library over Lombok**: Avoid Lombok where standard Java features (like Records and concise constructors) suffice, unless an entity strictly requires extensive boilerplate. Note that standard Maven compiler configs explicitly exclude lombok from Spring Boot.
- **Global Logging & Auditing**: You MUST use both `this.log.log()` and `console.log()` for the same log messages to ensure proper capture across different observability layers.
- **API Response Structure**: All project API responses MUST encapsulate their data within a standardized `Response` object pattern: `Response.builder().status(HttpStatus.OK).statusCode(HttpStatus.OK.value()).message("default.message.success").service("OPTIMIZE-SERVICE").data(data).build();`

**TypeScript/Next.js (Frontend):**
- **Strict Typing**: TypeScript `strict` mode is enabled. You MUST provide explicit types and avoid `any`.
- **Server vs Client Components**: Adhere strictly to Next.js 14 App Router conventions. Default to Server Components (`page.tsx`, `layout.tsx`) and explicitly use `"use client"` only when interactivity or client-side context (like hooks) is required.

### Framework-Specific Rules

**Spring Boot (Backend):**
- **Architecture Layers**: Strictly adhere to the standard `Controller -> Service -> Repository` layering. Do not put business logic in Controllers.
- **Data Encapsulation**: Controllers must accept and return DTOs, not raw Entity objects. Use mapping logic within the Service layer.
- **Dependency Injection**: Use constructor injection (via standard Java constructors) rather than `@Autowired` on fields, which facilitates easier testing.
- **Transaction Boundaries**: Apply `@Transactional` at the Service layer, not the Repository or Controller layers.

**Next.js (Frontend):**
- **App Router Paradigms**: All new routes should be created using the `app/` directory conventions, utilizing `page.tsx`, `layout.tsx`, and `loading.tsx` appropriately.
- **Component Organization**: Keep route files (`page.tsx`) focused on data fetching and layout structure. Extract complex UI logic into reusable components within `components/`.
- **Data Fetching**: Prefer Server Components for data fetching. Only use `useEffect` or SWR/React Query for data fetching when client-side interactivity explicitly demands it.

### Testing Rules

**Backend Testing:**
- **Integration Tests**: Rely on `Testcontainers` for integration testing (PostgreSQL, Kafka). Do not mock the database or message broker in integration tests.
- **Unit Tests**: Use JUnit 5 and Mockito for isolated unit tests (e.g., testing Service layer logic without starting the Spring Context).
- **Test Boundaries**: Keep unit tests fast and focused on pure business logic. Integration tests should verify the component interactions and configuration.

**Frontend Testing:**
- **Component Tests**: Use React Testing Library for component-level tests. Focus on user interactions and accessibility (`role` queries) rather than implementation details.
- **Mocking**: Mock external API calls and Next.js specific functions (like `next/navigation` or `next/router`) appropriately in isolated component tests.

### Code Quality & Style Rules

**Backend Rules:**
- **Immutability**: Prefer immutable DTOs (using Java `record`) and immutable collections.
- **Null Safety**: Rely on Spring's `@NonNull` and `@Nullable` annotations to clearly document and enforce nullability contracts in service interfaces.
- **REST Conventions**: Adhere strictly to RESTful naming conventions for APIs (e.g., plurals for resources, nouns instead of verbs in paths). Use standard HTTP status codes correctly.

**Frontend Rules:**
- **Tailwind Conventions**: Use Tailwind utility classes for styling. Avoid writing custom CSS in `globals.css` unless necessary for root variables or complex animations that are difficult to achieve with utility classes alone.
- **Component Props**: Define clear, explicit interfaces for all Component Props. Avoid spreading unknown props (`{...props}`) unless explicitly building a generic wrapper component.
- **Accessibility (a11y)**: Ensure all interactive elements have appropriate ARIA attributes and keyboard navigation support.

### Development Workflow Rules

**Branching & Commits:**
- **Branch Naming**: Use descriptive prefixes like `feature/`, `bugfix/`, or `chore/` followed by a concise description (e.g., `feature/add-tenant-api`).
- **Commit Messages**: Write clear, descriptive commit messages.

**Code Review & Dependencies:**
- **PR Checklists**: Ensure all new code meets the Testing and Code Quality rules before requesting review.
- **Dependency Management**: Only add new dependencies if absolutely necessary. Prefer using existing stack tools (e.g., use Postgres for queuing if Kafka is overkill for a small task).

### Critical Don't-Miss Rules

**Architecture & Implementation Pitfalls:**
- **UUID to Server ID Sync**: For new records created locally (e.g., clients), a UUID (string) MUST be used as a temporary primary key. During sync, this UUID MUST be replaced by the numeric ID returned by the server to prevent ID conflicts.
- **Angular Component Decorators**: When modifying an Angular component, NEVER remove the `standalone: false` property from the `@Component` decorator if it exists.
- **Service Naming**: When changing a service name, you MUST first verify that the new service name exists in the project. Do not assume a service name is a typo.
- **Map Marker Styling**: To change the map marker to red, implement an offline-first solution using a CSS-based `L.divIcon` in `map-preview.component.ts`.
- **API Response Abstraction**: Do not return raw data or generic ResponseEntity objects. You MUST use the `Response.builder()` pattern for all API endpoints.

---

## Usage Guidelines

**For AI Agents:**
- Read this file before implementing any code
- Follow ALL rules exactly as documented
- When in doubt, prefer the more restrictive option
- Update this file if new patterns emerge

**For Humans:**
- Keep this file lean and focused on agent needs
- Update when technology stack changes
- Review quarterly for outdated rules
- Remove rules that become obvious over time

Last Updated: {{date}}
