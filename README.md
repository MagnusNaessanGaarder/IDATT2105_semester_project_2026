# Internal Control System

Semesteroppgave i IDATT2105 - Fullstack applikasjon for internkontroll i matbedrifter.

## Teknologier

- **Backend:** Java 25, Spring Boot 4.0, MySQL 8.4, JWT
- **Frontend:** Vue 3, TypeScript, Vite, Pinia
- **Testing:** JUnit, Mockito, Vitest, Cypress

## Komme i gang

### Krav

- Java 25+
- Node.js 22+
- Docker & Docker Compose
- Maven

### Starte utviklingsmiljø

```bash
# Clone repo
git clone <repo-url>
cd IDATT2105_semester_project_2026

# Kopier env-malen
cp .env.example .env

# Start alt (MySQL + Backend + Frontend)
./start.sh
```

Applikasjonen er nå tilgjengelig på:
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

### Standard bruker

- Email: `admin@everest-sushi.no`
- Passord: `Test1234!`

## Testing

### Backend
```bash
cd backend
./mvnw test -Dtest="!*IntegrationTest"
```

### Frontend
```bash
cd frontend
npm run test:unit -- --run
npm run test:e2e
```

## Prosjektstruktur

```
.
├── backend/         # Spring Boot applikasjon
├── frontend/        # Vue.js applikasjon
├── docs/           # Dokumentasjon
├── compose-dev.yml # Docker Compose for utvikling
└── start.sh        # Hurtigstart-script
```

## CI/CD

GitHub Actions kjører automatisk tester ved push og pull requests:
- **Backend CI:** Kompilerer og kjører unit-tester
- **Frontend CI:** Kjører unit-tester og bygg

## Team

- Tri Tac Le
- [Andre teammedlemmer]

## Lisens

Privat - NTNU semesteroppgave
