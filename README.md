# WebTools Kanban Backend

A Spring Boot 3 backend for the WebTools Kanban board application. This service provides a REST API for managing a single Kanban board with columns and cards, featuring robust positional ordering and transactional board imports.

## 🚀 Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.4.2
- **Cloud**: Spring Cloud (Eureka, Config)
- **Build Tool**: Maven
- **Database**: PostgreSQL 17
- **Migrations**: Flyway
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Observability**: Spring Boot Actuator, Micrometer Tracing (Brave, Zipkin)

## 🛠️ Setup & Installation

### Prerequisites
- Java 21 JDK
- Maven
- Docker & Docker Compose

### Quick Start
1. **Start the Database**:
   ```bash
   docker compose up -d
   ```

2. **Run the Application**:
   ```bash
   mvn spring-boot:run
   ```

The server will start on `http://localhost:8080`.

## 📖 API Documentation

Once the application is running, you can access the interactive API documentation (Swagger UI) at:
`http://localhost:8080/swagger-ui/index.html`

### Base URL
`http://localhost:8080/api/v1`

### Key Endpoints
- `GET /board`: Fetch full board state.
- `POST /columns`: Create a new column.
- `DELETE /columns/{id}?transferTo={id}`: Delete a column and optionally transfer cards.
- `POST /cards`: Create a new card.
- `PATCH /cards/{id}/move`: Move a card within or across columns.
- `PUT /board/import`: Transactionally replace entire board state.

## 📉 Database Schema

- `board`: Top-level board metadata.
- `column_entity`: Columns belonging to a board, ordered by `position`.
- `card`: Cards belonging to a column, ordered by `position`. Labels are stored as `JSONB`.

## 🩺 Health Check
Check the service status:
`http://localhost:8080/actuator/health`
