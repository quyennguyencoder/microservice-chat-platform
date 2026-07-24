# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run Instructions

This is a multi-module Maven Spring Boot 3 / Java 21 project.

- **Build all modules:** `mvn clean install -DskipTests`
- **Build a specific module:** `mvn clean install -pl <module-name> -am -DskipTests` (e.g. `mvn clean install -pl chat-service -am -DskipTests`)
- **Run tests:** `mvn test`
- **Run tests for a single module:** `mvn test -pl <module-name>`
- **Run a single test class:** `mvn test -pl <module-name> -Dtest=ClassNameTest`
- **Run a single test method:** `mvn test -pl <module-name> -Dtest=ClassNameTest#methodName`

### Running the Application Locally

1. Start infrastructure services (PostgreSQL, Redis, Kafka, MinIO, Keycloak, ELK Stack, Prometheus, Grafana, Zipkin):
   `docker-compose up -d`
2. Start the Spring Boot microservices. They must be started in this strict order due to dependencies:
   - `mvn spring-boot:run -pl config-server` (wait until fully started)
   - `mvn spring-boot:run -pl discovery-server`
   - `mvn spring-boot:run -pl api-gateway`
   - Finally, start the domain services in separate terminals:
     - `mvn spring-boot:run -pl user-service`
     - `mvn spring-boot:run -pl chat-service`
     - `mvn spring-boot:run -pl search-service`
     - `mvn spring-boot:run -pl storage-service`
     - `mvn spring-boot:run -pl notification-service`

## Architecture Overview

ZChat is a real-time messaging platform built on a Microservices architecture.

### Key Components

- **`chat-service`**: The core messaging engine. It handles WebSocket connections (STOMP), chat room management, and messaging logic. It uses Redis Pub/Sub to synchronize WebSocket messages across multiple instances, enabling horizontal scalability.
- **`user-service`**: Manages user profiles and presence (online/offline status).
- **`search-service`**: Provides full-text search capabilities using Elasticsearch.
- **`storage-service`**: Manages file and media uploads/downloads using MinIO (S3 compatible).
- **`notification-service`**: Consumes events via Apache Kafka for push/email alerts.
- **`api-gateway`**: The single entry point for all client requests, handling routing and JWT authentication verification.
- **`common`**: A shared library module containing DTOs, exceptions, and event definitions used across services.

### Core Technologies

- **Java 21** & **Spring Boot 3.5.x** / **Spring Cloud 2025.0.x**
- **Authentication**: Keycloak (OAuth2 / OIDC / JWT).
- **Database**: PostgreSQL (via Spring Data JPA/Hibernate).
- **Event Streaming**: Apache Kafka for asynchronous communication between services (e.g., notifying `search-service` of new messages from `chat-service`).
- **Real-time Messaging**: Spring WebSocket with STOMP. Stateful connections are bound to local RAM, while Redis Pub/Sub handles cross-node message broadcasting.
- **Observability Stack**:
  - Metrics: Prometheus (scraping `/actuator/prometheus`) & Grafana.
  - Tracing: Zipkin & Micrometer Tracing (Brave).
  - Logging: Logstash (TCP ingestion) -> Elasticsearch -> Kibana.

### Development Guidelines

- **Stateless Services**: When working on real-time features (like WebSockets in `chat-service`), ensure the design supports horizontal scaling. Do not rely solely on local JVM memory (`ConcurrentHashMap`) for global state; use Redis to share state or broadcast events between instances.
- **Shared Code**: Reusable DTOs, Kafka event definitions (`ChatEvent`, `ChatEventType`), and custom exceptions should be placed in the `common` module.
- **Event-Driven Patterns**: Services communicate asynchronously via Kafka. When a significant domain event occurs (e.g., a message is sent or a group is updated), publish a Kafka event rather than making synchronous HTTP calls to other domain services.
