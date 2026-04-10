# Internal Control System (Internkontroll)

A full-stack web application for managing internal control systems in service businesses, with specialized modules for food safety (IK-Mat) and alcohol service compliance (IK-Alkohol).

> **IDATT2105 Fullstack Development - Semester Project**

## Features

### Core Modules

- **Authentication & Authorization**
  - JWT-based authentication with refresh tokens
  - Role-based access control (RBAC)
  - User management and permissions

- **Food Safety (IK-Mat)**
  - Temperature logging with threshold monitoring
  - HACCP checklist management
  - Deviation reporting and tracking
  - Daily control checklists

- **Alcohol Control (IK-Alkohol)**
  - Daily alcohol service controls
  - Staff certification tracking
  - Regulation compliance monitoring

- **Document Management**
  - File upload to Azure Blob Storage
  - Document versioning
  - Organization-specific document organization

- **Notifications & Audit**
  - Email notifications
  - Audit logging for compliance
  - Scheduled tasks for reminders and reports

- **Administration**
  - Organization management
  - System administrator panel
  - Export functionality for reports

## Tech Stack

### Backend
- **Java 21** with Spring Boot 3.3.5
- **Spring Security** with JWT authentication
- **Spring Data JPA** with MySQL
- **Flyway** for database migrations
- **Azure Blob Storage** for file storage
- **OpenAPI/Swagger** for API documentation
- **Maven** for build management

### Frontend
- **Vue 3** with Composition API and TypeScript
- **Vite** for build tooling
- **Vue Router** for navigation
- **Pinia** for state management
- **Axios** for API communication
- **Vitest** for unit testing
- **Cypress** for E2E testing

### Infrastructure
- **Docker & Docker Compose** for containerization
- **MySQL 8.4** database
- **Nginx** for production frontend serving

## Quick Start

### Prerequisites
- Java 21+
- Node.js 20.19+ or 22.12+
- Docker & Docker Compose
- Maven (wrapper included)

### Development Setup

1. **Clone and configure:**
```bash
git clone <repository-url>
cd IDATT2105_semester_project_2026

# Copy environment file
cp .env.example .env
# Edit .env with your configuration values
```

2. **Start all services:**
```bash
make dev
```

This starts:
- MySQL database in Docker
- Spring Boot backend on http://localhost:8080
- Vite dev server on http://localhost:5173

3. **Access the application:**
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

**Default test user:**
- Email: `admin@everest-sushi.no`
- Password: `Test1234!`

## Development Commands

### Using Make

```bash
make dev          # Start all services
make stop         # Stop all services
make status       # Check service status
make logs         # View logs
make test         # Run backend tests
make clean        # Reset everything
make install      # Install dependencies
```

### Manual Commands

**Backend:**
```bash
cd backend
./mvnw spring-boot:run -DskipTests          # Start server
./mvnw test -Dcheckstyle.skip=true          # Run tests
./mvnw jacoco:report -Dcheckstyle.skip=true # Coverage report
./mvnw checkstyle:checkstyle                # Code style check
```

**Frontend:**
```bash
cd frontend
npm install          # Install dependencies
npm run dev          # Start dev server
npm run test:unit    # Run unit tests
npm run test:e2e     # Run E2E tests
npm run lint         # Run linters
npm run build        # Production build
```

## Project Structure

```
├── backend/                    # Spring Boot application
│   ├── src/main/java/...
│   │   ├── controller/        # REST API controllers
│   │   ├── service/           # Business logic
│   │   ├── repository/        # Data access layer
│   │   ├── model/             # Entity models
│   │   ├── dto/               # Data transfer objects
│   │   ├── security/          # JWT & security config
│   │   └── config/            # Application config
│   └── src/main/resources/
│       └── db/migration/      # Flyway migrations
│
├── frontend/                   # Vue 3 application
│   ├── src/
│   │   ├── features/          # Feature modules
│   │   │   ├── auth/          # Authentication
│   │   │   ├── dashboard/     # Main dashboard
│   │   │   ├── ik-mat/        # Food safety module
│   │   │   ├── ik-alkohol/    # Alcohol control module
│   │   │   ├── admin/         # Admin panel
│   │   │   └── sysadmin/      # System admin
│   │   ├── shared/            # Shared components
│   │   ├── router/            # Vue Router config
│   │   └── stores/            # Pinia stores
│   └── cypress/               # E2E tests
│
├── compose-dev.yaml           # Docker Compose for dev
├── Makefile                   # Development commands
└── .env.example               # Environment template
```

## Testing

### Backend Tests
```bash
cd backend
./mvnw test -Dcheckstyle.skip=true
```

### Frontend Unit Tests
```bash
cd frontend
npm run test:unit -- --run
```

### Frontend E2E Tests
```bash
cd frontend
npm run test:e2e
```

### API Tests
```bash
# Using curl
cd backend
./test-api-curl.sh

# Using HTTPie (if installed)
./test-api-httpie.sh
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | MySQL connection URL |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `JWT_SECRET_KEY` | JWT signing key |
| `AZURE_STORAGE_CONNECTION_STRING` | Azure Blob Storage connection |
| `SPRING_MAIL_HOST` | SMTP server host |
| `USE_MOCK` | Enable mock mode (true/false) |

See `.env.example` for complete list.

## Production Deployment

1. **Build production images:**
```bash
make prod-build
```

2. **Start production services:**
```bash
make prod-up
```

3. **View production logs:**
```bash
make prod-logs
```

4. **Stop production:**
```bash
make prod-down
```

## API Documentation

Once the backend is running, view the full API documentation at:
http://localhost:8080/swagger-ui.html

## Troubleshooting

### Port Already in Use
```bash
make stop
# Or manually:
lsof -ti:8080 | xargs kill -9
lsof -ti:5173 | xargs kill -9
```

### Database Reset
```bash
# Warning: This deletes all data!
docker stop backend-mysql-1
docker rm backend-mysql-1
docker volume rm idatt2105_semester_project_2026_mysql-dev-data
```

### Flyway Migration Errors
If you get "checksum mismatch" after changing SQL files:
```bash
# Reset the database (see above)
# Then restart with: make dev
```

### View Logs
```bash
# Backend
tail -f /tmp/backend.log

# Frontend
tail -f /tmp/frontend.log

# MySQL
docker logs backend-mysql-1
```
