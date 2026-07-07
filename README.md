# Smart Notification Management System (Backend API)

This repository contains the production-ready Spring Boot 3.x backend application for the Smart Notification Management System, built using Java 17, Maven, PostgreSQL, and Liquibase database versioning.

---

## Technical Stack
- **Core Runtime**: Java 17 (OpenJDK 17+)
- **Framework**: Spring Boot 3.3.1 (Web, JPA, Validation)
- **Database**: PostgreSQL
- **Migration & Versioning**: Liquibase Core
- **Mappers & Utilities**: MapStruct 1.5.5.Final, Lombok 1.18.46
- **Build Tool**: Maven 3.x

---

## Project Architecture

The application adopts a clean, layered architecture separating concerns into independent layers:

```
[Client Request]
       │
       ▼
[OncePerRequestFilter] (RequestLoggingFilter)
       │
       ▼
[Controller Layer] (NotificationController, DashboardController)
       │
       ▼
[Service Layer] (NotificationService, NotificationServiceImpl)
       │
  ┌────┴──────────────────────────┐
  ▼                               ▼
[Repository Layer]       [Async Task Executor] (ExecutorService)
(NotificationRepository)          │
  ▲                               ▼
  │                     [Queue / Process Layer] (NotificationProcessor)
  └───────────────────────────────┘
```

1. **API Filters**: Centrally intercepts HTTP request metadata (remote IP, method, request URI, response statuses, and execution durations).
2. **Controller Layer**: Exposes REST API endpoints, parses parameters, maps request/response payloads using MapStruct, and performs input validations.
3. **Service Layer**: Implements business constraints (consecutive repeated words validation, duplicate notification rules, retry checks).
4. **Queue / Process Layer**: Simulates background network transmission asynchronously using a spring-managed fixed thread pool `ExecutorService` (avoiding RabbitMQ as per requirements).
5. **Database/Repository Layer**: Performs JPA/Hibernate actions over PostgreSQL databases using optimized index queries.

---

## Database Schema & Migrations

Database structures are version-controlled using Liquibase and located in `src/main/resources/db/changelog/`.
Startup schema changes are run automatically on application initialization.

### Notifications Table Schema
- `id` (UUID, Primary Key)
- `user_id` (BIGINT, NOT NULL)
- `type` (VARCHAR(20), NOT NULL) - Supported: `EMAIL`, `SMS`, `PUSH`
- `message` (TEXT, NOT NULL)
- `status` (VARCHAR(20), NOT NULL) - Supported: `PENDING`, `SENT`, `FAILED`, `RETRYING`
- `retry_count` (INTEGER, DEFAULT 0, NOT NULL)
- `schedule_time` (TIMESTAMP, NULLABLE)
- `created_at` (TIMESTAMP, NOT NULL)
- `updated_at` (TIMESTAMP, NOT NULL)
- `last_retry_time` (TIMESTAMP, NULLABLE)

### Schema Indexes
- **Single Columns**: Indexes are created on `status`, `type`, `user_id`, and `created_at` to guarantee fast pagination filtering.
- **Unique Business Index**: A composite index is added on `(user_id, type, message, created_at)` to optimize performance of the duplicate notification query.

---

## Business & Design Assumptions

1. **Duplicate Notification Limit**: A notification is classified as a duplicate if the same user attempts to send the same message type and content within **5 minutes**.
2. **Message Content Validation**: Rejects any request carrying messages where any single word is repeated **more than 3 consecutive times** (e.g. `hello hello hello hello` is rejected).
3. **Transmission Simulation**: Asynchronous delivery of notification tasks marks **70% as SENT** and **30% as FAILED** randomly.
4. **Retry Cooldown & Limits**: Retrying a failed notification is restricted to cases where:
   - Current status is `FAILED`
   - Total retries are less than 3
   - Time elapsed since the last retry is **at least 2 minutes**.
5. **Deduplicated Transaction Safety**: Asynchronous workers are submitted after the transaction commits successfully using Spring's `TransactionSynchronizationManager` to guarantee that threads do not read uncommitted records.

---

## Setup & Running Instructions

### 1. Prerequisites
- **JDK 17** installed and configured in system path.
- **Maven 3.x** installed.
- **PostgreSQL** running instance.

### 2. Configure Environment Variables
The application properties are configured with fallbacks but can be overriden via the command line or shell variables:

| Environment Variable | Default Value | Description |
|---|---|---|
| `DB_HOST` | `localhost` | Database Hostname |
| `DB_PORT` | `5432` | Database Port |
| `DB_NAME` | `notification_db` | Target PostgreSQL Database Name |
| `DB_USERNAME` | `postgres` | Database Username |
| `DB_PASSWORD` | `postgres` | Database Password |
| `SERVER_PORT` | `8080` | Server Listening Port |

### 3. Build the Application
Compile the sources, run annotation processors, and package the application into a bootable JAR:
```bash
mvn clean package -DskipTests
```

### 4. Run the Application
Start the Spring Boot application on port `8080` (with `/api` context path):
```bash
java -jar target/notification-management-api-0.0.1-SNAPSHOT.jar
```
*Note: Make sure your target PostgreSQL database (e.g. `notification_db`) is created prior to running so Liquibase can create the schema on startup.*