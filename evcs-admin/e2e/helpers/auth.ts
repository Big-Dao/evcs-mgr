import { expect, type Page } from '@playwright/test'

const identifier = process.env.EVCS_E2E_IDENTIFIER || 'admin@tenant1'
const password = process.env.EVCS_E2E_PASSWORD || 'password'

export const loginAsAdmin = async (page: Page) => {
  await page.goto('/login')
  await expect(page.getByTestId('login-form')).toBeVisible()

  await page.getByTestId('login-identifier').fill(identifier)
  await page.getByTestId('login-password').fill(password)
  await page.getByTestId('login-submit').click()

  await expect(page).toHaveURL(/\/(dashboard)?(?:\?.*)?$/)
}