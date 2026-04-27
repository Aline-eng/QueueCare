package com.queuecare.queuecare;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

class AuthApiTest extends BaseIntegrationTest {

    // ── Happy Path ────────────────────────────────────────────────────────────

    @Test
    void register_withValidPatient_returns200AndDefaultsRoleToPatient() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Alice\",\"email\":\"alice.auth@test.com\",\"password\":\"pass123\"}")
            .post("/auth/register")
            .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("role", equalTo("PATIENT"));
    }

    @Test
    void register_withExplicitStaffRole_returnsStaff() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Bob\",\"email\":\"bob.auth@test.com\",\"password\":\"pass123\",\"role\":\"STAFF\"}")
            .post("/auth/register")
            .then()
            .statusCode(200)
            .body("role", equalTo("STAFF"));
    }

    @Test
    void login_withValidCredentials_returnsToken() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Carol\",\"email\":\"carol.auth@test.com\",\"password\":\"pass123\"}")
            .post("/auth/register");

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"carol.auth@test.com\",\"password\":\"pass123\"}")
            .post("/auth/login")
            .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("token", not(emptyString()));
    }

    // ── Negative Cases ────────────────────────────────────────────────────────

    @Test
    void login_withWrongPassword_returns401() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Dave\",\"email\":\"dave.auth@test.com\",\"password\":\"pass123\"}")
            .post("/auth/register");

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"dave.auth@test.com\",\"password\":\"wrongpass\"}")
            .post("/auth/login")
            .then()
            .statusCode(401);
    }

    @Test
    void login_withNonExistentEmail_returns401() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"ghost@test.com\",\"password\":\"pass123\"}")
            .post("/auth/login")
            .then()
            .statusCode(401);
    }

    @Test
    void register_withDuplicateEmail_returns409() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Eve\",\"email\":\"eve.auth@test.com\",\"password\":\"pass123\"}")
            .post("/auth/register");

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Eve2\",\"email\":\"eve.auth@test.com\",\"password\":\"pass123\"}")
            .post("/auth/register")
            .then()
            .statusCode(409);
    }

    @Test
    void accessProtectedEndpoint_withNoToken_returns401() {
        RestAssured.given()
            .get("/appointments")
            .then()
            .statusCode(401);
    }

    @Test
    void accessProtectedEndpoint_withInvalidToken_returns401() {
        RestAssured.given()
            .header("Authorization", "Bearer not-a-real-token")
            .get("/appointments")
            .then()
            .statusCode(401);
    }

    // ── Edge Cases ────────────────────────────────────────────────────────────

    @Test
    void register_withMissingName_returns400() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"noname@test.com\",\"password\":\"pass123\"}")
            .post("/auth/register")
            .then()
            .statusCode(400);
    }

    @Test
    void register_withInvalidEmailFormat_returns400() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Bad\",\"email\":\"not-an-email\",\"password\":\"pass123\"}")
            .post("/auth/register")
            .then()
            .statusCode(400);
    }

    @Test
    void register_withMissingPassword_returns400() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"NoPass\",\"email\":\"nopass@test.com\"}")
            .post("/auth/register")
            .then()
            .statusCode(400);
    }

    @Test
    void login_withEmptyBody_returns400() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{}")
            .post("/auth/login")
            .then()
            .statusCode(400);
    }
}
