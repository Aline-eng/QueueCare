package com.queuecare.queuecare;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueueCareUiTest extends BaseUiTest {

    @Test
    void loginFlow_validCredentials_navigatesToAppointments() {
        registerUser("Lina Valid", "lina.valid@test.com", "pass123", null);

        page.navigate(baseUrl() + "/login.html");
        page.getByTestId("email-input").fill("lina.valid@test.com");
        page.getByTestId("password-input").fill("pass123");
        page.getByTestId("login-button").click();

        page.waitForURL("**/patient-dashboard.html");
        assertTrue(page.url().endsWith("/patient-dashboard.html"));
        assertTrue(page.getByTestId("appointments-list").isVisible());
    }

    @Test
    void loginFlow_invalidCredentials_showsError() {
        registerUser("Ivy Invalid", "ivy.invalid@test.com", "pass123", null);

        page.navigate(baseUrl() + "/login.html");
        page.getByTestId("email-input").fill("ivy.invalid@test.com");
        page.getByTestId("password-input").fill("wrongpass");
        page.getByTestId("login-button").click();

        page.getByTestId("error-message").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        assertEquals("Invalid email or password.", page.getByTestId("error-message").textContent().trim());
    }

    @Test
    void loginFlow_emptySubmission_showsValidationMessage() {
        page.navigate(baseUrl() + "/login.html");
        page.getByTestId("login-button").click();

        page.getByTestId("error-message").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        assertEquals("Email and password are required.", page.getByTestId("error-message").textContent().trim());
    }

    @Test
    void createAppointment_submitForm_bookingAppearsInList() {
        registerUser("Dr. Carter", "dr.carter@test.com", "pass123", "STAFF");
        registerUser("Ava Booker", "ava.booker@test.com", "pass123", null);

        page.navigate(baseUrl() + "/login.html");
        page.getByTestId("email-input").fill("ava.booker@test.com");
        page.getByTestId("password-input").fill("pass123");
        page.getByTestId("login-button").click();
        page.waitForURL("**/patient-dashboard.html");

        String date = futureDate(15);
        page.getByTestId("doctor-input").selectOption("Dr. Carter");
        page.getByTestId("date-input").fill(date);
        page.getByTestId("reason-input").fill("Dental cleaning");
        page.getByTestId("book-button").click();

        page.getByTestId("book-success").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        Locator row = page.getByTestId("appointments-list").locator("tr").filter(new Locator.FilterOptions().setHasText("Dr. Carter"));
        row.waitFor();
        assertTrue(row.textContent().contains("Dental cleaning"));
    }

    @Test
    void appointmentForm_emptyFieldsAndInvalidInput_showValidationMessages() {
        registerUser("Dr. Adams", "dr.adams@test.com", "pass123", "STAFF");
        registerUser("Mia Validate", "mia.validate@test.com", "pass123", null);

        page.navigate(baseUrl() + "/login.html");
        page.getByTestId("email-input").fill("mia.validate@test.com");
        page.getByTestId("password-input").fill("pass123");
        page.getByTestId("login-button").click();
        page.waitForURL("**/patient-dashboard.html");

        page.getByTestId("book-button").click();
        page.getByTestId("book-error").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        assertEquals("All fields are required.", page.getByTestId("book-error").textContent().trim());

        page.getByTestId("doctor-input").selectOption("Dr. Adams");
        page.getByTestId("date-input").fill("2020-01-01");
        page.getByTestId("reason-input").fill("Past date check");
        page.getByTestId("book-button").click();

        page.getByTestId("book-error").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        assertTrue(page.getByTestId("book-error").textContent().contains("Appointment date"));
    }

    @Test
    void updateAppointment_saveChanges_reflectsInUi() {
        registerUser("Dr. Green", "dr.green@test.com", "pass123", "STAFF");
        registerUser("Nora Update", "nora.update@test.com", "pass123", null);

        page.navigate(baseUrl() + "/login.html");
        page.getByTestId("email-input").fill("nora.update@test.com");
        page.getByTestId("password-input").fill("pass123");
        page.getByTestId("login-button").click();
        page.waitForURL("**/patient-dashboard.html");

        String originalDate = futureDate(20);
        String updatedDate = futureDate(25);
        page.getByTestId("doctor-input").selectOption("Dr. Green");
        page.getByTestId("date-input").fill(originalDate);
        page.getByTestId("reason-input").fill("Initial visit");
        page.getByTestId("book-button").click();

        page.getByTestId("book-success").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        Locator row = page.getByTestId("appointments-list").locator("tr").filter(new Locator.FilterOptions().setHasText("Dr. Green"));
        row.waitFor();
        String rowId = row.getAttribute("data-testid").replace("appointment-row-", "");

        page.getByTestId("edit-button-" + rowId).click();
        page.getByTestId("edit-modal").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        page.getByTestId("edit-doctor-input").selectOption("Dr. Green");
        page.getByTestId("edit-date-input").fill(updatedDate);
        page.getByTestId("edit-reason-input").fill("Follow-up review");
        page.getByTestId("edit-save-button").click();

        Locator updatedRow = page.getByTestId("appointment-row-" + rowId);
        updatedRow.waitFor();
        waitForText(updatedRow, "Follow-up review");
        assertTrue(updatedRow.textContent().contains("Follow-up review"));
    }

    @Test
    void cancelAppointment_confirmDialog_updatesStatusInUi() {
        registerUser("Dr. White", "dr.white@test.com", "pass123", "STAFF");
        registerUser("Cora Cancel", "cora.cancel@test.com", "pass123", null);

        page.navigate(baseUrl() + "/login.html");
        page.getByTestId("email-input").fill("cora.cancel@test.com");
        page.getByTestId("password-input").fill("pass123");
        page.getByTestId("login-button").click();
        page.waitForURL("**/patient-dashboard.html");

        String date = futureDate(30);
        page.getByTestId("doctor-input").selectOption("Dr. White");
        page.getByTestId("date-input").fill(date);
        page.getByTestId("reason-input").fill("Cancel me");
        page.getByTestId("book-button").click();

        page.getByTestId("book-success").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        Locator row = page.getByTestId("appointments-list").locator("tr").filter(new Locator.FilterOptions().setHasText("Dr. White"));
        row.waitFor();
        String rowId = row.getAttribute("data-testid").replace("appointment-row-", "");

        page.onDialog(dialog -> dialog.accept());
        page.getByTestId("cancel-button-" + rowId).click();

        Locator canceledRow = page.getByTestId("appointment-row-" + rowId);
        canceledRow.waitFor();
        waitForText(canceledRow, "CANCELED");
        assertTrue(canceledRow.textContent().contains("CANCELED"));
    }

    private void waitForText(Locator locator, String expectedText) {
        for (int i = 0; i < 20; i++) {
            String text = locator.textContent();
            if (text != null && text.contains(expectedText)) {
                return;
            }
            page.waitForTimeout(150);
        }
    }
}
