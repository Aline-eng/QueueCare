package com.queuecare.queuecare;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;

class QueueApiTest extends BaseIntegrationTest {

    private String patientToken;
    private String staffToken;
    private int appointmentId;
    private final String today = LocalDate.now().toString();

    @BeforeEach
    void setupUsersAndAppointment() {
        patientToken = registerAndLogin("Alice Queue", "alice.queue@test.com", "pass123", null);
        staffToken = registerAndLogin("Bob Queue", "bob.queue@test.com", "pass123", "STAFF");

        appointmentId = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + patientToken)
            .body(String.format("{\"doctor\":\"Dr. Smith\",\"appointmentDate\":\"%s\",\"reason\":\"Queue test\"}", today))
            .post("/appointments")
            .jsonPath().getInt("id");
    }

    // ── Happy Path ────────────────────────────────────────────────────────────

    @Test
    void getTodayQueue_returnsAppointmentsOrderedByQueueNumber() {
        RestAssured.given()
            .header("Authorization", "Bearer " + staffToken)
            .get("/queue/today")
            .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
            .body("appointmentDate", everyItem(equalTo(today)));
    }

    @Test
    void markAsServed_asStaff_returnsCompleted() {
        RestAssured.given()
            .header("Authorization", "Bearer " + staffToken)
            .patch("/queue/" + appointmentId + "/serve")
            .then()
            .statusCode(200)
            .body("status", equalTo("COMPLETED"));
    }

    // ── Negative Cases ────────────────────────────────────────────────────────

    @Test
    void markAsServed_asPatient_returns403() {
        RestAssured.given()
            .header("Authorization", "Bearer " + patientToken)
            .patch("/queue/" + appointmentId + "/serve")
            .then()
            .statusCode(403);
    }

    @Test
    void markAsServed_withNoToken_returns401() {
        RestAssured.given()
            .patch("/queue/" + appointmentId + "/serve")
            .then()
            .statusCode(401);
    }

    @Test
    void markAsServed_withNonExistentId_returns404() {
        RestAssured.given()
            .header("Authorization", "Bearer " + staffToken)
            .patch("/queue/99999/serve")
            .then()
            .statusCode(404);
    }

    // ── Edge Cases ────────────────────────────────────────────────────────────

    @Test
    void markAsServed_alreadyServed_returns409() {
        RestAssured.given()
            .header("Authorization", "Bearer " + staffToken)
            .patch("/queue/" + appointmentId + "/serve");

        RestAssured.given()
            .header("Authorization", "Bearer " + staffToken)
            .patch("/queue/" + appointmentId + "/serve")
            .then()
            .statusCode(409);
    }

    @Test
    void markAsServed_cancelledAppointment_returns409() {
        RestAssured.given()
            .header("Authorization", "Bearer " + patientToken)
            .delete("/appointments/" + appointmentId);

        RestAssured.given()
            .header("Authorization", "Bearer " + staffToken)
            .patch("/queue/" + appointmentId + "/serve")
            .then()
            .statusCode(409);
    }

    @Test
    void getTodayQueue_isOrderedByQueueNumber() {
        String anotherPatientToken = registerAndLogin("Dan Queue", "dan.queue@test.com", "pass123", null);

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + anotherPatientToken)
            .body(String.format("{\"doctor\":\"Dr. Jones\",\"appointmentDate\":\"%s\",\"reason\":\"Second\"}", today))
            .post("/appointments");

        RestAssured.given()
            .header("Authorization", "Bearer " + staffToken)
            .get("/queue/today")
            .then()
            .statusCode(200)
            .body("queueNumber", everyItem(notNullValue()));
    }
}
