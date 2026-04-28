# QueueCare

A clinic appointment system with queue management, role-based access, and a full automated test suite.

## Prerequisites

- Java 21
- Maven (or use the included `mvnw` wrapper)
- PostgreSQL 14+ (for running the app; tests use H2 in-memory)
- Node.js 18+ and npm (for UI tests)

## Stack

- Backend: Java 21 + Spring Boot 3.2
- Auth: Bearer token (UUID-based, in-memory store) + BCrypt password hashing
- Database: PostgreSQL (production), H2 (tests)
- API Tests: RestAssured + JUnit 5
- UI Tests: Playwright (Chromium)

## Setup

### 1. Database

Create a PostgreSQL database named `queuecare`:

```sql
CREATE DATABASE queuecare;
```

### 2. Environment variables (optional)

The app defaults to `localhost:5432` with user/password `postgres`. Override with:

```
QUEUECARE_DB_URL=jdbc:postgresql://localhost:5432/queuecare
QUEUECARE_DB_USERNAME=postgres
QUEUECARE_DB_PASSWORD=postgres
```

### 3. Start the application

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`. Opening that URL redirects to the login page.

## Default test credentials

The app has no seed data. Register via the UI at `/register.html` or via the API:

```bash
# Register a patient
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","password":"password123"}'

# Register staff
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Bob Staff","email":"bob@example.com","password":"password123","role":"STAFF"}'
```

## Running API tests

Tests use H2 in-memory — no database setup needed.

```bash
./mvnw test
```

All 38 tests should pass.

## Running UI tests

The application must be running before executing UI tests.

```bash
# 1. Start the app (in one terminal)
./mvnw spring-boot:run

# 2. Install Playwright dependencies (first time only)
cd e2e
npm install
npx playwright install chromium

# 3. Run UI tests
npm test
```

To view an HTML report after the run:

```bash
npm run test:report
npx playwright show-report
```

## API overview

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/auth/register` | Public | Register a new user |
| POST | `/auth/login` | Public | Login, receive token |
| GET | `/appointments` | Any role | Patient: own only; Staff/Admin: all |
| POST | `/appointments` | Any role | Create appointment |
| GET | `/appointments/{id}` | Owner or Staff/Admin | Get by ID |
| PUT | `/appointments/{id}` | Owner or Staff/Admin | Update |
| DELETE | `/appointments/{id}` | Owner or Staff/Admin | Cancel |
| GET | `/queue/today` | Staff/Admin only | Today's queue ordered by queue number |
| PATCH | `/queue/{id}/serve` | Staff/Admin only | Mark patient as served |

## Roles

- `PATIENT` — manages own appointments only
- `STAFF` — views and manages all appointments, marks patients as served
- `ADMIN` — same as STAFF
