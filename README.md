# ZChat - Enterprise Microservices Chat Platform

ZChat is a modern, real-time messaging platform built on a **Microservices** architecture powered by **Spring Boot 3** and **Spring Cloud**. Designed for high scalability, resilience, and cloud-native observability, ZChat provides seamless 1-on-1 and group communication with enterprise-grade monitoring and infrastructure.

---

## 🌟 Key Features

- **Real-Time Messaging**: Instant 1-on-1 and group chat powered by **WebSocket / STOMP**.
- **Authentication & Security**: Centralized Identity & Access Management (SSO) via **Keycloak** (OAuth2 / OpenID Connect / JWT).
- **Global Full-Text Search**: Fast and accurate message and user discovery using **Elasticsearch 8**.
- **Object Storage**: Secure file and media attachments management using **MinIO** (S3-compatible object storage).
- **Asynchronous Event Processing**: Reliable push notifications and event-driven communication via **Apache Kafka**.
- **Enterprise Observability Stack**: Complete **Direct Integration** monitoring covering the Three Pillars of Observability:
  - **Metrics**: Real-time JVM, system, and custom business metrics scraped via **Prometheus** and visualized on **Grafana**.
  - **Tracing**: Distributed request tracing across microservices with **Zipkin** and **Micrometer Tracing (Brave)**.
  - **Centralized Logging**: Structured JSON log aggregation via **Logstash** TCP ingestion, stored in **Elasticsearch**, and analyzed on **Kibana**.
  - **Automated Alerting**: 24/7 proactive system monitoring with automated alerts sent to **Telegram** via Grafana.

---

## 🏗 System Architecture

The platform is structured into core services and supporting infrastructure:

### 1. Backend Microservices (Spring Boot & Spring Cloud)
- **`api-gateway`**: Single entry point for clients; handles request routing, CORS, and JWT authentication.
- **`discovery-server`**: Service registry and discovery powered by **Netflix Eureka**.
- **`config-server`**: Centralized configuration management for all microservices.
- **`user-service`**: Manages user profiles, social relationships, and online/offline presence status.
- **`chat-service`**: Core chat engine handling messaging logic, chat rooms, and WebSocket delivery.
- **`search-service`**: Synchronizes messaging data and exposes full-text search endpoints via Elasticsearch.
- **`storage-service`**: Manages file uploads, downloads, and media metadata via MinIO.
- **`notification-service`**: Event-driven notification consumer handling real-time push and email alerts via Kafka.
- **`common`**: Shared library containing common DTOs, utilities, exceptions, and observability configurations.

### 2. Infrastructure & Observability (Docker Compose)
- **PostgreSQL 17**: Primary relational database for user data, chat metadata, and Keycloak realm persistence.
- **Redis 8**: High-performance in-memory caching and session management.
- **Apache Kafka 7.8**: Distributed event streaming platform and message broker.
- **MinIO**: High-performance S3-compatible object storage.
- **Keycloak 26**: Open-source identity and access management provider.
- **Elasticsearch 8**: Distributed search and analytics engine for search and centralized log storage.
- **Logstash 8**: Log ingestion pipeline listening on TCP port 5000 for structured JSON logs.
- **Kibana 8**: Log analytics and exploration UI.
- **Prometheus**: Time-series database scraping `/actuator/prometheus` endpoints.
- **Grafana**: Advanced visualization dashboards and automated alerting engine.
- **Zipkin**: Distributed tracing waterfall visualization.

---

## 🚀 Getting Started

### Prerequisites
- **Java Development Kit (JDK) 21**
- **Docker & Docker Compose**
- **Apache Maven 3.9+**

### 1. Launch Infrastructure & Observability Stack
Navigate to the project root directory and start all infrastructure services using Docker Compose:

```bash
docker-compose up -d
```
> [!NOTE]
> This command starts PostgreSQL, Redis, Kafka, MinIO, Keycloak, and the entire Observability Stack (Elasticsearch, Logstash, Kibana, Prometheus, Grafana, and Zipkin). Initialization scripts (`init-db.sql` and `realm-export.json`) will automatically set up the database and Keycloak realm.

### 2. Launch Microservices
Start the backend microservices in the following strict order to ensure proper dependency resolution:

1. **`config-server`** (Wait until started and serving configurations)
2. **`discovery-server`** (Eureka Registry)
3. **`api-gateway`**
4. **Domain Services**: `user-service`, `chat-service`, `search-service`, `storage-service`, `notification-service`

You can run individual microservices via your IDE (e.g., IntelliJ IDEA) or using the Maven Spring Boot plugin:
```bash
mvn spring-boot:run -pl <module-name>
```

---

## 🌐 Local Access & Port Directory

| Service / Tool | Port | Local URL | Default Credentials | Description |
| :--- | :---: | :--- | :---: | :--- |
| **API Gateway** | `8000` | http://localhost:8000 | — | Main API entry point for frontend clients |
| **Eureka Dashboard** | `8761` | http://localhost:8761 | — | Microservices registry and status monitor |
| **Keycloak Admin** | `9090` | http://localhost:9090 | `admin` / `admin` | Identity and IAM administration |
| **MinIO Console** | `9001` | http://localhost:9001 | `minioadmin` / `minioadmin` | Object storage bucket management |
| **Grafana UI** | `3001` | http://localhost:3001 | `admin` / `admin` | Metrics dashboards and Telegram alerting |
| **Kibana UI** | `5601` | http://localhost:5601 | — | Centralized log search and analytics |
| **Zipkin UI** | `9411` | http://localhost:9411 | — | Distributed tracing and latency analysis |
| **Prometheus** | `9091` | http://localhost:9091 | — | Time-series metrics query interface |

---

## 📖 Observability & Documentation

For detailed guides on operating the monitoring stack, debugging distributed traces, searching logs with KQL, and setting up automated Telegram alerts, refer to our comprehensive architecture documentation:
- [Observability User Guide](docs/architecture/observability_user_guide.md)
- [Direct Observability Stack Migration](docs/architecture/migration_to_direct_observability_stack.md)
- [The Three Pillars of Observability](docs/architecture/observability_three_pillars.md)

---

## 🛠 Tech Stack

- **Core & Framework**: Java 21, Spring Boot 3.5.x, Spring Cloud 2025.0.x
- **Security**: Spring Security, OAuth2 Resource Server, Keycloak JWT
- **Data & Storage**: Spring Data JPA, Hibernate, PostgreSQL, Redis, MinIO
- **Messaging & Event Streaming**: WebSocket, STOMP, Apache Kafka
- **Observability**: Micrometer, Brave, Prometheus, Grafana, Zipkin, ELK Stack (Elasticsearch, Logstash, Kibana)
- **DevOps**: Docker, Docker Compose, Maven

---

## 📄 License

This project is developed for educational and professional enterprise architecture demonstration purposes.
