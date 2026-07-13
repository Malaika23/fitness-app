import { test, expect } from '@playwright/test';

test('E2E Flow - Register New User, Log In, and Log Activity', async ({ page }) => {
  const timestamp = Date.now();
  const username = `testuser_${timestamp}`;
  const email = `testuser_${timestamp}@example.com`;

  // 1. Navigate to the frontend application
  await page.goto('http://localhost:5173');

  // 2. Click LOGIN button
  const loginButton = page.locator('button:has-text("LOGIN")');
  if (await loginButton.isVisible()) {
    await loginButton.click();
  }

  // 3. Wait for redirect to Keycloak login page
  await page.waitForURL(/.*:8181.*/, { timeout: 15000 });

  // 4. Click the "Register" link on the Keycloak page
  // Keycloak typically uses a link with text "Register" or link matching register url
  const registerLink = page.locator('a:has-text("Register")');
  await expect(registerLink).toBeVisible({ timeout: 10000 });
  await registerLink.click();

  // 5. Fill out the Keycloak registration form
  await page.waitForSelector('#firstName', { timeout: 10000 });
  await page.fill('#firstName', 'E2E');
  await page.fill('#lastName', 'NewUser');
  await page.fill('#email', email);
  await page.fill('#username', username);
  await page.fill('#password', 'Password123!');
  await page.fill('#password-confirm', 'Password123!');

  // Click the register submit button (typically value="Register" or text="Register")
  const registerSubmit = page.locator('input[type="submit"]');
  await registerSubmit.click();

  // 6. Wait for redirect back to application activities page (User should be auto-logged in)
  await page.waitForURL(/.*\/activities/, { timeout: 15000 });

  // 7. Verify we are on the activities dashboard and log a new activity
  await page.click('div[role="combobox"]');
  await page.click('li[data-value="WALKING"]');

  await page.getByLabel('Duration (Minutes)').fill('40');
  await page.getByLabel('Calories Burned').fill('350');

  // Submit the activity form
  await page.locator('button:has-text("Add Activity")').click();

  // 8. Verify the activity is added to the list and click it
  const activityCard = page.locator('text=WALKING').first();
  await expect(activityCard).toBeVisible({ timeout: 10000 });
  await activityCard.click();

  // 9. Verify details and loading of dynamic AI recommendation
  await page.waitForURL(/.*\/activities\/.+/, { timeout: 10000 });
  await expect(page.locator('text=Activity Details')).toBeVisible();
  
  const aiRecommendationHeader = page.locator('text=AI Recommendation');
  await expect(aiRecommendationHeader).toBeVisible({ timeout: 15000 });
  await expect(page.getByRole('heading', { name: 'Analysis' })).toBeVisible();
});
