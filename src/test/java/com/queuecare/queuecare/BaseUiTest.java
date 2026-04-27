package com.queuecare.queuecare;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.queuecare.queuecare.repository.AppointmentRepository;
import com.queuecare.queuecare.repository.UserRepository;
import com.queuecare.queuecare.service.TokenService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
public abstract class BaseUiTest {

    private static Playwright playwright;
    private static Browser browser;

    @LocalServerPort
    int port;

    @Autowired
    AppointmentRepository appointmentRepository;

    @Autowired
    UserRepository userRepository;

    BrowserContext context;
    Page page;

    @BeforeAll
    static void startBrowser() {
        playwright = Playwright.create();
        browser = launchBrowser(playwright);
    }

    @AfterAll
    static void stopBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void setUpUiTest() {
        appointmentRepository.deleteAll();
        userRepository.deleteAll();
        TokenService.clearTokens();

        RestAssured.port = port;
        RestAssured.basePath = "";

        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 960));
        page = context.newPage();
    }

    @AfterEach
    void tearDownUiTest() {
        if (context != null) {
            context.close();
        }
    }

    String baseUrl() {
        return "http://localhost:" + port;
    }

    void registerUser(String name, String email, String password, String role) {
        String body = role != null
                ? String.format("{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}", name, email, password, role)
                : String.format("{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}", name, email, password);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/auth/register")
                .then()
                .statusCode(200);
    }

    String futureDate(int plusDays) {
        return java.time.LocalDate.now().plusDays(plusDays).toString();
    }

    private static Browser launchBrowser(Playwright playwright) {
        BrowserType.LaunchOptions headedEdge = new BrowserType.LaunchOptions()
                .setChannel("msedge")
                .setHeadless(true);
        try {
            return playwright.chromium().launch(headedEdge);
        } catch (RuntimeException ignored) {
        }

        BrowserType.LaunchOptions chrome = new BrowserType.LaunchOptions()
                .setChannel("chrome")
                .setHeadless(true);
        try {
            return playwright.chromium().launch(chrome);
        } catch (RuntimeException ignored) {
        }

        Path edgePath = Path.of("C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe");
        if (Files.exists(edgePath)) {
            return playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setExecutablePath(edgePath)
                    .setHeadless(true));
        }

        return playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }
}
