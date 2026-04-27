package com.queuecare.queuecare;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

class AppointmentApiTest extends BaseIntegrationTest {

    private String patientToken;
    private String staffToken;
    private String anotherPatientToken;

    @BeforeEach
    void setupUsers() {
        patientToken = registerAndLogin("Alice", "alice.appt@test.com", "pass123", null);
        staffToken = registerAndLogin("Bob Staff", "bob.appt@test.com", "pass123", "STAFF");
        anotherPatientToken = registerAndLogin("Charlie", "charlie.appt@test.com", "pass123", null);
    }

    // ── Happy Path ────────────────────────────────────────────────────────────

    @Test
    void createAppointment_withValidData_returns201WithQueueNumber() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. Smith\",\"appointmentDate\":\"2026-12-01\",\"reason\":\"Checkup\"}")
            .post("/appointments")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("queueNumber", notNullValue())
            .body("status", equalTo("SCHEDULED"));
    }

    @Test
    void getAppointments_asPatient_returnsOnlyOwnAppointments() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. Smith\",\"appointmentDate\":\"2026-12-02\",\"reason\":\"Checkup\"}")
            .post("/appointments");

        RestAssured.given()
            .header("Authorization", "Bearer " + patientToken)
            .get("/appointments")
            .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
            .body("patientName", everyItem(equalTo("Alice")));
    }

    @Test
    void getAppointments_asStaff_returnsAllAppointments() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. Smith\",\"appointmentDate\":\"2026-12-03\",\"reason\":\"Checkup\"}")
            .post("/appointments");

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + anotherPatientToken)
            .body("{\"doctor\":\"Dr. Jones\",\"appointmentDate\":\"2026-12-04\",\"reason\":\"Flu\"}")
            .post("/appointments");

        RestAssured.given()
            .header("Authorization", "Bearer " + staffToken)
            .get("/appointments")
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(2));
    }

    @Test
    void getAppointmentById_asOwner_returns200() {
        int id = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. Smith\",\"appointmentDate\":\"2026-12-05\",\"reason\":\"Checkup\"}")
            .post("/appointments")
            .jsonPath().getInt("id");

        RestAssured.given()
            .header("Authorization", "Bearer " + patientToken)
            .get("/appointments/" + id)
            .then()
            .statusCode(200)
            .body("id", equalTo(id));
    }

    @Test
    void updateAppointment_asOwner_returns200WithUpdatedFields() {
        int id = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. Smith\",\"appointmentDate\":\"2026-12-06\",\"reason\":\"Checkup\"}")
            .post("/appointments")
            .jsonPath().getInt("id");

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. Johnson\",\"appointmentDate\":\"2026-12-20\",\"reason\":\"Follow-up\"}")
            .put("/appointments/" + id)
            .then()
            .statusCode(200)
            .body("doctor", equalTo("Dr. Johnson"))
            .body("reason", equalTo("Follow-up"));
    }

    @Test
    void cancelAppointment_asOwner_returnsStatusCanceled() {
        int id = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. Smith\",\"appointmentDate\":\"2026-12-07\",\"reason\":\"Checkup\"}")
            .post("/appointments")
            .jsonPath().getInt("id");

        RestAssured.given()
            .header("Authorization", "Bearer " + patientToken)
            .delete("/appointments/" + id)
            .then()
            .statusCode(200)
            .body("status", equalTo("CANCELED"));
    }

    // ── Negative Cases ────────────────────────────────────────────────────────

    @Test
    void getAppointments_withNoToken_returns401() {
        RestAssured.given()
            .get("/appointments")
            .then()
            .statusCode(401);
    }

    @Test
    void getAppointmentById_withNonExistentId_returns404() {
        RestAssured.given()
            .header("Authorization", "Bearer " + patientToken)
            .get("/appointments/99999")
            .then()
            .statusCode(404);
    }

    @Test
    void getAppointmentById_asAnotherPatient_returns403() {
        int id = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. Smith\",\"appointmentDate\":\"2026-12-08\",\"reason\":\"Checkup\"}")
            .post("/appointments")
            .jsonPath().getInt("id");

        RestAssured.given()
            .header("Authorization", "Bearer " + anotherPatientToken)
            .get("/appointments/" + id)
            .then()
            .statusCode(403);
    }

    @Test
    void cancelAppointment_alreadyCancelled_returns409() {
        int id = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. Smith\",\"appointmentDate\":\"2026-12-09\",\"reason\":\"Checkup\"}")
            .post("/appointments")
            .jsonPath().getInt("id");

        RestAssured.given()
            .header("Authorization", "Bearer " + patientToken)
            .delete("/appointments/" + id);

        RestAssured.given()
            .header("Authorization", "Bearer " + patientToken)
            .delete("/appointments/" + id)
            .then()
            .statusCode(409);
    }

    @Test
    void updateAppointment_alreadyCancelled_returns409() {
        int id = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. Smith\",\"appointmentDate\":\"2026-12-10\",\"reason\":\"Checkup\"}")
            .post("/appointments")
            .jsonPath().getInt("id");

        RestAssured.given()
            .header("Authorization", "Bearer " + patientToken)
            .delete("/appointments/" + id);

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. X\",\"appointmentDate\":\"2026-12-21\",\"reason\":\"Test\"}")
            .put("/appointments/" + id)
            .then()
            .statusCode(409);
    }

    // ── Edge Cases ────────────────────────────────────────────────────────────

    @Test
    void createAppointment_withPastDate_returns400() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. Smith\",\"appointmentDate\":\"2020-01-01\",\"reason\":\"Past\"}")
            .post("/appointments")
            .then()
            .statusCode(400);
    }

    @Test
    void createAppointment_withMissingFields_returns400() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. Smith\"}")
            .post("/appointments")
            .then()
            .statusCode(400);
    }

    @Test
    void createAppointment_duplicateSameDay_returns409() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. Smith\",\"appointmentDate\":\"2026-12-11\",\"reason\":\"First\"}")
            .post("/appointments");

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. Adams\",\"appointmentDate\":\"2026-12-11\",\"reason\":\"Second\"}")
            .post("/appointments")
            .then()
            .statusCode(409);
    }

    @Test
    void createAppointment_afterCancellation_sameDayAllowed() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. Smith\",\"appointmentDate\":\"2026-12-12\",\"reason\":\"First\"}")
            .post("/appointments");

        int id = RestAssured.given()
            .header("Authorization", "Bearer " + patientToken)
            .get("/appointments")
            .jsonPath().getInt("find { it.appointmentDate == '2026-12-12' }.id");

        RestAssured.given()
            .header("Authorization", "Bearer " + patientToken)
            .delete("/appointments/" + id);

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. Adams\",\"appointmentDate\":\"2026-12-12\",\"reason\":\"Rebook\"}")
            .post("/appointments")
            .then()
            .statusCode(201);
    }

    @Test
    void updateAppointment_toPastDate_returns400() {
        int id = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. Smith\",\"appointmentDate\":\"2026-12-13\",\"reason\":\"Checkup\"}")
            .post("/appointments")
            .jsonPath().getInt("id");

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body("{\"doctor\":\"Dr. Smith\",\"appointmentDate\":\"2020-01-01\",\"reason\":\"Past\"}")
            .put("/appointments/" + id)
            .then()
            .statusCode(400);
    }
}
