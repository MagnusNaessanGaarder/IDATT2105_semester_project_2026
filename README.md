# Internal Control System - foreløpig README for at dåkk skal kun å kjør applikasjonen

Hvis nånn andre gruppa ser på REPOET og kopiere koden e dåkk støgg - det kan få oss utvist (æ satt inn script som sir github bruker te kæm som skjer repoet og sett at flere av dåkk homsa har sett på repoet)
Semester project for IDATT2105 - Fullstack application for internal control in service businesses.

## Quick Start (dev)

1. **Setup environment:**

```bash
# Copy environment file (ask Tri for .env values)
cp .env.example .env
```

Remember to give perms to shell scripts using chmod +x

2. **Start all services:**

```bash
./start.sh
```

3. **Access the application (dev):**

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html

**Test user:**

- Email: `admin@everest-sushi.no`
- Password: `Test1234!`

4. **Stop all services:**

```bash
./stop.sh
# Or press Ctrl+C in the terminal where start.sh is running
```

---

## Development

### Prerequisites

- Java 25+
- Node.js 22+
- Docker & Docker Compose
- Maven (included via wrapper)

### Running Locally

**Option 1: Using start.sh (Recommended)**

```bash
./start.sh
```

This starts:

- MySQL in Docker (automatically managed by Spring Boot)
- Spring Boot backend on port 8080
- Vite frontend dev server on port 5173

**Option 2: Manual start**

Terminal 1 - Backend:

```bash
cd backend
./mvnw spring-boot:run -DskipTests
```

Terminal 2 - Frontend:

```bash
cd frontend
npm run dev
```

### VPN Configuration

If you use VPN edit `compose-dev.yaml`:

```yaml
# VPN mode (uncomment this):
network_mode: host

# Standard mode (comment out):
# ports:
#   - '3306:3306'
```

Then restart with `./stop.sh && ./start.sh`

---

## Testing

### Backend Tests

```bash
cd backend

# Run all tests
./mvnw test -Dcheckstyle.skip=true

# Run with coverage report
./mvnw jacoco:report -Dcheckstyle.skip=true
# Open: backend/target/site/jacoco/index.html

# Check code style
./mvnw checkstyle:checkstyle
```

### Frontend Tests

```bash
cd frontend

# Unit tests
npm run test:unit -- --run

# E2E tests
npm run test:e2e
```

---

## API Testing

### Automated API Tests

**Option 1: Using curl (default - everyone has this)**

```bash
cd backend
./test-api.sh
```

**Option 2: Using HTTPie (more readable output, requires installation)**

```bash
# Install HTTPie first:
# sudo pacman -S httpie

cd backend
./test-api-httpie.sh
```

### Manual API Testing

## Troubleshooting

### Port already in use

```bash
./stop.sh  # Stops all services
# Or manually:
lsof -ti:8080 | xargs kill -9
lsof -ti:5173 | xargs kill -9
```

### Database issues

```bash
# Reset database (deletes all data!)
docker stop backend-mysql-1
docker rm backend-mysql-1
./start.sh
```

### Check logs

```bash
# Backend logs
tail -f /tmp/backend.log

# Frontend logs
tail -f /tmp/frontend.log

# MySQL logs
docker logs backend-mysql-1
```

### Flyway migration errors

If you get "checksum mismatch" errors after changing SQL files:

```bash
# Reset database completely
docker stop backend-mysql-1
docker rm backend-mysql-1
docker volume rm idatt2105_semester_project_2026_mysql-dev-data
./start.sh
```
