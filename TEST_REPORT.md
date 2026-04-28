# Test Report

## What I Built

A REST API clinic appointment system with a plain HTML frontend.

**Stack:** Java 21, Spring Boot 3.2, Spring Security, Spring Data JPA, PostgreSQL (H2 for tests), Lombok.

**Key decisions:**
- Token auth using UUID tokens stored in a static in-memory map (`TokenService`). Simple and sufficient for this scope — not production-grade.
- BCrypt password hashing via Spring Security's `BCryptPasswordEncoder`.
- Role-based access enforced at the service layer, not via Spring Security method annotations, to keep the logic explicit and testable.
- Queue numbers are assigned per-day and skip cancelled appointments, so cancelling and rebooking correctly recalculates the queue position.
- Duplicate booking on the same day is blocked unless the existing appointment is cancelled.

---

## What I Tested

### API (RestAssured + JUnit 5) — 38 tests

**Auth (12 tests)**
- Register with valid data, explicit role, default role
- Login with valid credentials returns token
- Login with wrong password → 401
- Login with non-existent email → 401
- Duplicate email registration → 409
- Protected endpoint with no token → 401
- Protected endpoint with invalid token → 401
- Register with missing name, missing password, invalid email format → 400
- Login with empty body → 400

**Appointments (17 tests)**
- Create appointment → 201 with queue number assigned
- Patient sees only own appointments; staff sees all
- Get by ID as owner → 200
- Update appointment → 200 with updated fields
- Cancel appointment → status CANCELED
- No token → 401
- Non-existent ID → 404
- Another patient's appointment → 403
- Cancel already-cancelled → 409
- Update already-cancelled → 409
- Past date on create → 400
- Missing fields → 400
- Invalid date format → 400
- Duplicate booking same day → 409
- Rebook after cancellation same day → 201 (allowed)
- Reschedule to past date → 400

**Queue (8 tests)**
- Today's queue returns appointments ordered by queue number
- Staff marks patient as served → COMPLETED
- Patient tries to mark as served → 403
- No token → 401
- Non-existent ID → 404
- Mark already-served → 409
- Mark cancelled appointment as served → 409
- Queue ordering verified across multiple patients

### UI (Playwright, Chromium) — 9 tests

- Login with valid credentials redirects to appointments page
- Login with invalid credentials shows error message
- Empty login form shows error message
- Book appointment form — fill and submit, booking appears in list
- Book appointment with empty form shows validation error
- Book appointment with past date shows server error
- Edit appointment — updated values appear in list
- Cancel appointment — status changes to CANCELED
- Staff login shows queue section

---

## What I Automated vs Manual

All scenarios above are automated. The following were not automated:

- **Postman collection** (`postman/QueueCare_Auth.postman_collection.json`) covers the same API scenarios and can be run manually via Newman. It was built before the RestAssured suite and kept as an additional artefact.
- **Cross-browser UI testing** — only Chromium is covered. Firefox and WebKit were not added to keep the setup simple.
- **Load/concurrency testing** — queue number assignment uses a count-based approach that is not atomic under concurrent writes. Not tested.

---

## Bugs Found

### 1. Queue number race condition (known, not fixed)
`nextQueueNumber()` counts existing non-cancelled appointments for a date and adds 1. Under concurrent requests for the same date, two patients could receive the same queue number. This is a known limitation of the in-memory count approach. A database sequence or pessimistic lock would fix it.

### 2. Token store is not persistent across restarts
Tokens are stored in a static `HashMap`. Restarting the server invalidates all active sessions. Users must log in again. Documented, not fixed — acceptable for this scope.

### 3. Password exposed in register response
`POST /auth/register` returns the full `User` entity including the BCrypt-hashed password. The hash is not a secret, but it is unnecessary information in the response. A dedicated `UserResponse` DTO should be used instead.

### 4. No token expiry
Tokens never expire. A logged-in user's token is valid indefinitely until the server restarts. In production this would require expiry and refresh logic.

---

## What I Would Improve Given More Time

- Replace the UUID token store with JWT (stateless, expiry built-in)
- Add a `UserResponse` DTO to avoid leaking the password hash in the register response
- Make queue number assignment atomic (database sequence or `SELECT FOR UPDATE`)
- Add pagination to `GET /appointments` for staff — returning all records is fine for small data but would not scale
- Add Firefox and WebKit to the Playwright config
- Add a `@BeforeAll` database seed for UI tests instead of registering via API in each test's `beforeEach`
