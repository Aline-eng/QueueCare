# Test Report

## What I Built

A REST API clinic appointment system with a plain HTML frontend and role-based dashboards.

**Stack:** Java 21, Spring Boot 3.2, Spring Security, Spring Data JPA, PostgreSQL (H2 for tests), Lombok, RestAssured, Playwright for Java.

**Key decisions:**

- Token auth using UUID tokens stored in a static in-memory map (`TokenService`). Simple and sufficient for this scope — not production-grade.
- BCrypt password hashing via Spring Security's `BCryptPasswordEncoder`.
- Role-based access enforced at the service layer, not via Spring Security method annotations, to keep the logic explicit and testable.
- Queue numbers are assigned per-day based on the count of existing appointments for that date. Cancelling and rebooking correctly recalculates the queue position.
- Duplicate booking on the same day is blocked unless the existing appointment is cancelled.
- Three separate HTML dashboards — patient, staff, admin — each rendered based on the role returned by `GET /auth/me` after login.
- Passwords are never returned in API responses — a dedicated `UserResponse` DTO is used throughout.

---

## What I Tested

### API (RestAssured + JUnit 5) — 37 tests

**Auth — 12 tests**
- Register with valid data, explicit role, default role (PATIENT)
- Login with valid credentials returns token
- Login with wrong password → 401
- Login with non-existent email → 401
- Duplicate email registration → 409
- Protected endpoint with no token → 401
- Protected endpoint with invalid token → 401
- Register with missing name → 400
- Register with missing password → 400
- Register with invalid email format → 400
- Login with empty body → 400

**Appointments — 17 tests**
- Create appointment → 201 with queue number assigned and status SCHEDULED
- Patient sees only own appointments; staff sees all
- Get by ID as owner → 200
- Update appointment → 200 with updated fields reflected
- Cancel appointment → status CANCELED
- No token on protected endpoint → 401
- Non-existent appointment ID → 404
- Patient accesses another patient's appointment → 403
- Cancel already-cancelled appointment → 409
- Update already-cancelled appointment → 409
- Create with past date → 400
- Create with missing fields → 400
- Create with invalid date format → 400
- Duplicate booking same day → 409
- Rebook after cancellation on same day → 201 (allowed)
- Reschedule to past date → 400
- Invalid date format on create → 400

**Queue — 8 tests**
- Today's queue returns appointments for today ordered by queue number
- Staff marks patient as served → status COMPLETED
- Patient tries to mark as served → 403
- No token on serve endpoint → 401
- Non-existent appointment ID on serve → 404
- Mark already-served patient as served again → 409
- Mark cancelled appointment as served → 409
- Queue ordering verified across multiple patients on same day

### UI (Playwright, Chromium/Edge headless) — 7 tests

- Login with valid credentials → redirects to correct role dashboard
- Login with invalid credentials → shows error message
- Empty login form submission → shows validation error
- Book appointment — fill form and submit → booking appears in appointments list
- Book appointment with empty form → shows validation error
- Book appointment with past date → shows date validation error
- Edit appointment → updated values reflected in the list
- Cancel appointment → status changes to CANCELED in the list

---

## What I Automated vs Manual

All scenarios above are fully automated and run with `mvnw test`.

The following were not automated:

- **Cross-browser UI testing** — only Chromium/Edge is covered. Firefox and WebKit were not added to keep the setup simple.
- **Load and concurrency testing** — queue number assignment is not atomic under concurrent writes. Not tested.
- **Admin and staff UI flows** — UI tests cover the patient dashboard only. Admin and staff dashboard interactions (user management, mark served from UI) were tested manually.

The **Postman collection** (`postman/QueueCare_Auth.postman_collection.json`) covers the same API scenarios and can be run manually via Newman. It was built before the RestAssured suite and kept as an additional artefact.

---

## Bugs Found

### 1. Queue number race condition (known, not fixed)
`AppointmentService.create()` counts existing non-cancelled appointments for a date and adds 1 to assign the queue number. Under concurrent requests for the same date, two patients could receive the same queue number. A database sequence or `SELECT FOR UPDATE` would fix this.

### 2. Token store not persistent across restarts
Tokens are stored in a static `HashMap`. Restarting the server invalidates all active sessions — users must log in again. Acceptable for this scope but not production-ready.

### 3. No token expiry
Tokens never expire. A logged-in user's token is valid indefinitely until the server restarts. Production would require expiry and refresh logic.

### 4. Queue number not recalculated on cancellation
When an appointment is cancelled, the queue numbers of subsequent patients for that day are not shifted down. Gaps appear in the queue (e.g. 1, 3, 4 if position 2 is cancelled). This is a known limitation — fixing it would require reordering all remaining appointments for the day.

---

## What I Would Improve Given More Time

- Replace the UUID token store with JWT (stateless, expiry built-in, no server-side store needed)
- Make queue number assignment atomic using a database sequence or pessimistic lock
- Recalculate queue numbers when an appointment is cancelled to avoid gaps
- Add pagination to `GET /appointments` for staff — returning all records does not scale
- Add Firefox and WebKit to the Playwright configuration
- Add UI tests for the staff and admin dashboards
- Add integration tests for the `UserController` (admin user management endpoints)
