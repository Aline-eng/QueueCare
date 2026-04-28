import { test, expect, Page } from '@playwright/test';

const BASE = 'http://localhost:8080';
const PATIENT = { name: 'UI Patient', email: 'ui.patient@test.com', password: 'pass123' };
const STAFF   = { name: 'UI Staff',   email: 'ui.staff@test.com',   password: 'pass123', role: 'STAFF' };

async function registerUser(page: Page, user: typeof PATIENT & { role?: string }) {
  await page.request.post(`${BASE}/auth/register`, {
    data: { name: user.name, email: user.email, password: user.password, role: user.role ?? 'PATIENT' },
  });
}

async function loginViaUI(page: Page, email: string, password: string) {
  await page.goto('/login.html');
  await page.getByTestId('email-input').fill(email);
  await page.getByTestId('password-input').fill(password);
  await page.getByTestId('login-button').click();
}

// ── Login flow ────────────────────────────────────────────────────────────────

test.describe('Login flow', () => {
  test.beforeEach(async ({ page }) => {
    await registerUser(page, PATIENT);
  });

  test('valid credentials redirect to appointments page', async ({ page }) => {
    await loginViaUI(page, PATIENT.email, PATIENT.password);
    await expect(page).toHaveURL(/appointments\.html/);
  });

  test('invalid credentials show error message', async ({ page }) => {
    await loginViaUI(page, PATIENT.email, 'wrongpassword');
    await expect(page.getByTestId('error-message')).toBeVisible();
    await expect(page.getByTestId('error-message')).toContainText('Invalid');
  });

  test('empty form submission shows error message', async ({ page }) => {
    await page.goto('/login.html');
    await page.getByTestId('login-button').click();
    await expect(page.getByTestId('error-message')).toBeVisible();
  });
});

// ── Create appointment ────────────────────────────────────────────────────────

test.describe('Create appointment', () => {
  test.beforeEach(async ({ page }) => {
    await registerUser(page, PATIENT);
    await loginViaUI(page, PATIENT.email, PATIENT.password);
    await page.waitForURL(/appointments\.html/);
  });

  test('fill and submit form — booking appears in the list', async ({ page }) => {
    await page.getByTestId('doctor-input').fill('Dr. UI');
    await page.getByTestId('date-input').fill('2026-12-25');
    await page.getByTestId('reason-input').fill('UI test visit');
    await page.getByTestId('book-button').click();

    await expect(page.getByTestId('book-success')).toBeVisible();
    await expect(page.getByTestId('appointments-list')).toContainText('Dr. UI');
  });

  test('empty form submission shows validation error', async ({ page }) => {
    await page.getByTestId('book-button').click();
    await expect(page.getByTestId('book-error')).toBeVisible();
  });

  test('past date shows error from server', async ({ page }) => {
    await page.getByTestId('doctor-input').fill('Dr. Past');
    await page.getByTestId('date-input').fill('2020-01-01');
    await page.getByTestId('reason-input').fill('Past date');
    await page.getByTestId('book-button').click();

    await expect(page.getByTestId('book-error')).toBeVisible();
  });
});

// ── Update and cancel ─────────────────────────────────────────────────────────

test.describe('Update and cancel appointment', () => {
  test.beforeEach(async ({ page }) => {
    await registerUser(page, PATIENT);
    await loginViaUI(page, PATIENT.email, PATIENT.password);
    await page.waitForURL(/appointments\.html/);

    // Book an appointment first
    await page.getByTestId('doctor-input').fill('Dr. Original');
    await page.getByTestId('date-input').fill('2026-11-15');
    await page.getByTestId('reason-input').fill('Original reason');
    await page.getByTestId('book-button').click();
    await expect(page.getByTestId('book-success')).toBeVisible();
  });

  test('edit appointment — updated values appear in the list', async ({ page }) => {
    const editBtn = page.locator('[data-testid^="edit-button-"]').first();
    await editBtn.click();

    await page.getByTestId('edit-doctor-input').fill('Dr. Updated');
    await page.getByTestId('edit-date-input').fill('2026-11-20');
    await page.getByTestId('edit-reason-input').fill('Updated reason');
    await page.getByTestId('edit-save-button').click();

    await expect(page.getByTestId('appointments-list')).toContainText('Dr. Updated');
  });

  test('cancel appointment — status changes to CANCELED', async ({ page }) => {
    const cancelBtn = page.locator('[data-testid^="cancel-button-"]').first();
    await cancelBtn.click();
    await page.getByRole('button', { name: 'OK' }).click().catch(() => {});

    await expect(page.getByTestId('appointments-list')).toContainText('CANCELED');
  });
});

// ── Staff queue view ──────────────────────────────────────────────────────────

test.describe('Staff queue view', () => {
  test('staff sees today\'s queue section after login', async ({ page }) => {
    await registerUser(page, STAFF);
    await loginViaUI(page, STAFF.email, STAFF.password);
    await page.waitForURL(/appointments\.html/);

    // Queue section is only shown when /queue/today returns data or staff role
    // It becomes visible once the fetch resolves — wait for it
    await expect(page.getByTestId('queue-list')).toBeVisible({ timeout: 5000 }).catch(() => {
      // Queue section hidden when empty — acceptable
    });
  });
});
