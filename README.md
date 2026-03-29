# Internal Control System

Semester project for IDATT2105 - Fullstack application for internal control in service businesses.

## Quick Start

Spør Tri om .env
cp .env.example .env

# Start all services (MySQL + Backend + Frontend)

./start.sh

````

Access the application:
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html

**Test user:**
- Email: `admin@everest-sushi.no`
- Password: `Test1234!`

## Testing

**Backend:**
```bash
cd backend
./mvnw test
````

**Frontend:**

```bash
cd frontend
npm run test:unit -- --run
npm run test:e2e
```

## Requirements

- Java 25+
- Node.js 22+
- Docker & Docker Compose
- Maven
