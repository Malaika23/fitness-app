import { test, expect } from '@playwright/test';

test('E2E Flow - Create Custom Workout and Verify AI Recommendation', async ({ page }) => {
  // 1. Navigate to the landing page
  await page.goto('http://localhost:5173');

  // 2. If landing page is shown, click LOGIN. (If auto-redirected to Keycloak, wait for redirect)
  const loginButton = page.locator('button:has-text("LOGIN")');
  if (await loginButton.isVisible()) {
    await loginButton.click();
  }

  // 3. Wait for the Keycloak sign-in page and authenticate
  await page.waitForURL(/.*:8181.*/, { timeout: 15000 });
  await page.fill('#username', 'user2');
  await page.fill('#password', 'Password123!');
  await page.click('#kc-login');

  // 4. Wait for redirect back to application activities page
  await page.waitForURL(/.*\/activities/, { timeout: 15000 });

  // 5. Fill out the activity form
  // Click on the select dropdown for Activity Type
  await page.click('div[role="combobox"]');
  // Click the 'Other' menu item
  await page.click('li[data-value="OTHER"]');

  // Wait for the custom name input field to appear and fill it
  await page.getByLabel('Custom Activity Name').fill('Playwright Test');

  // Fill in duration and calories
  await page.getByLabel('Duration (Minutes)').fill('30');
  await page.getByLabel('Calories Burned').fill('200');

  // Submit the form
  await page.locator('button:has-text("Add Activity")').click();

  // 6. Verify that the activity is added to the list and click it
  const newCard = page.locator('text=OTHER (Playwright Test)').first();
  await expect(newCard).toBeVisible({ timeout: 10000 });
  await newCard.click();

  // 7. Verify the details and that the AI recommendation displays
  await page.waitForURL(/.*\/activities\/.+/, { timeout: 10000 });
  await expect(page.locator('text=Activity Details')).toBeVisible();
  await expect(page.locator('text=Other (Playwright Test)')).toBeVisible();

  // Wait for the AI Recommendations section to render
  const aiRecommendationHeader = page.locator('text=AI Recommendation');
  await expect(aiRecommendationHeader).toBeVisible({ timeout: 15000 });
  await expect(page.getByRole('heading', { name: 'Analysis' })).toBeVisible();
});
