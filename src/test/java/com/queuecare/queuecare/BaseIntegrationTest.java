package com.queuecare.queuecare;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.queuecare.queuecare.repository.AppointmentRepository;
import com.queuecare.queuecare.repository.UserRepository;
import com.queuecare.queuecare.service.TokenService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
public abstract class BaseIntegrationTest {

    @LocalServerPort
    int port;

    @org.springframework.beans.factory.annotation.Autowired
    AppointmentRepository appointmentRepository;

    @org.springframework.beans.factory.annotation.Autowired
    UserRepository userRepository;

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        userRepository.deleteAll();
        TokenService.clearTokens();
        RestAssured.port = port;
        RestAssured.basePath = "";
    }

    protected String registerAndLogin(String name, String email, String password, String role) {
        String body = role != null
            ? String.format("{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}", name, email, password, role)
            : String.format("{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}", name, email, password);

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(body)
            .post("/auth/register");

        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password))
            .post("/auth/login")
            .jsonPath().getString("token");
    }
}
