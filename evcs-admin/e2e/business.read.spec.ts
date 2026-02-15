import { test, expect } from '@playwright/test'
import { loginAsAdmin } from './helpers/auth'

test.describe('core read paths', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page)
  })

  test('stations list should be accessible and loaded', async ({ page }) => {
    await page.goto('/stations')

    await expect(page).toHaveURL(/\/stations(?:\?.*)?$/)
    await expect(page.getByTestId('station-page-title')).toBeVisible()
    await expect(page.getByTestId('station-table')).toBeVisible()
    await expect(page.getByText('加载充电站列表失败，显示模拟数据')).toHaveCount(0)
  })

  test('orders list should be accessible and loaded', async ({ page }) => {
    await page.goto('/orders')

    await expect(page).toHaveURL(/\/orders(?:\?.*)?$/)
    await expect(page.getByTestId('order-page-title')).toBeVisible()
    await expect(page.getByTestId('order-table')).toBeVisible()
    await expect(page.getByText('加载订单列表失败，显示模拟数据')).toHaveCount(0)
  })

  test('payments list should be accessible and loaded', async ({ page }) => {
    await page.goto('/payments')

    await expect(page).toHaveURL(/\/payments(?:\?.*)?$/)
    await expect(page.getByTestId('payment-page-title')).toBeVisible()
    await expect(page.getByTestId('payment-table')).toBeVisible()
    await expect(page.getByText('加载支付列表失败，显示模拟数据')).toHaveCount(0)
  })
})