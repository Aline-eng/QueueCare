# QueueCare

A clinic appointment management system with queue management, role-based dashboards, and a full automated test suite.

## Prerequisites

- Java 21
- Maven (or use the included `mvnw` wrapper)
- PostgreSQL 14+ (for running the app — tests use H2 in-memory, no PostgreSQL needed for tests)
- A Chromium or Edge browser installed (for UI tests)

## Stack

- Backend: Java 21 + Spring Boot 3.2
- Auth: Bearer token (UUID-based, in-memory store) + BCrypt password hashing
- Database: PostgreSQL (production), H2 in-memory (tests)
- API Tests: RestAssured + JUnit 5
- UI Tests: Playwright for Java (Chromium/Edge)

## Setup

### 1. Create the database

```sql
CREATE DATABASE queuecare;
```

### 2. Configure credentials

The app defaults to `localhost:5432`, username `postgres`. Set these environment variables to override:

```
QUEUECARE_DB_URL=jdbc:postgresql://localhost:5432/queuecare
QUEUECARE_DB_USERNAME=postgres
QUEUECARE_DB_PASSWORD=your_password
```

Or edit `src/main/resources/application.properties` directly.

### 3. Start the application

```bash
./mvnw spring-boot:run        # Linux / macOS
mvnw.cmd spring-boot:run      # Windows
```

The app starts on `http://localhost:8080` and redirects to the login page.

## Default test credentials

The app has no seed data. Use the register page or the API to create users.

```bash
# Register a patient (via UI: http://localhost:8080/register.html)
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice Patient","email":"alice@example.com","password":"password123"}'

# Register a staff / doctor
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Dr. Smith","email":"smith@example.com","password":"password123","role":"STAFF"}'

# Register an admin
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Admin User","email":"admin@example.com","password":"password123","role":"ADMIN"}'
```

> **Note:** The public register page (`/register.html`) only creates PATIENT accounts. Staff and Admin accounts must be created via the API or through the Admin dashboard once an admin account exists.

## Running all tests

Tests use H2 in-memory — no PostgreSQL setup needed.

```bash
./mvnw test        # Linux / macOS
mvnw.cmd test      # Windows
```

Expected result: **45 tests, 0 failures, 0 errors**

| Suite | Tests |
|-------|-------|
| AuthApiTest | 12 |
| AppointmentApiTest | 17 |
| QueueApiTest | 8 |
| QueueCareUiTest | 7 |
| QueuecareApplicationTests | 1 |

UI tests launch a headless Chromium or Edge browser automatically. The test runner detects whichever is installed on the machine.

## Postman collection

A Postman collection is included at `postman/QueueCare_Auth.postman_collection.json`.

Import it into Postman and run the collection in order (Auth → Appointments → Queue). The Login request auto-saves the token to a collection variable used by all subsequent requests.

## API overview

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/auth/register` | Public | Register a new user |
| POST | `/auth/login` | Public | Login, receive token |
| GET | `/auth/me` | Any | Get current user info |
| GET | `/appointments` | Any | Patient: own only; Staff/Admin: all |
| POST | `/appointments` | Any | Create appointment |
| GET | `/appointments/{id}` | Owner or Staff/Admin | Get by ID |
| PUT | `/appointments/{id}` | Owner or Staff/Admin | Update |
| DELETE | `/appointments/{id}` | Owner or Staff/Admin | Cancel |
| GET | `/queue/today` | Any authenticated | Today's queue ordered by queue number |
| PATCH | `/queue/{id}/serve` | Staff/Admin only | Mark patient as served |
| GET | `/users` | Admin only | List all users |
| DELETE | `/users/{id}` | Admin only | Delete a user |
| GET | `/users/doctors` | Any authenticated | List all staff (for doctor dropdown) |

## Roles and dashboards

| Role | Dashboard | Capabilities |
|------|-----------|--------------|
| `PATIENT` | `/patient-dashboard.html` | Book appointments (doctor dropdown), view own appointments, edit/cancel scheduled appointments |
| `STAFF` | `/staff-dashboard.html` | View today's queue filtered to their name, mark patients as served, view all their appointments |
| `ADMIN` | `/admin-dashboard.html` | View all appointments, manage all users (create/delete), view and manage today's full queue |

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `QUEUECARE_DB_URL` | `jdbc:postgresql://localhost:5432/queuecare` | JDBC URL |
| `QUEUECARE_DB_USERNAME` | `postgres` | DB username |
| `QUEUECARE_DB_PASSWORD` | *(set via environment variable)* | DB password |

> Never commit real credentials. Set `QUEUECARE_DB_PASSWORD` as an environment variable in your shell or deployment platform.

## UI

The frontend is built with plain HTML and JavaScript — no frameworks or component libraries.
All pages are in [`src/main/resources/static/`](https://github.com/Aline-eng/QueueCare/tree/main/src/main/resources/static)

| Page | Path |
|------|------|
| Login | `/login.html` |
| Register | `/register.html` |
| Patient Dashboard | `/patient-dashboard.html` |
| Staff Dashboard | `/staff-dashboard.html` |
| Admin Dashboard | `/admin-dashboard.html` |
