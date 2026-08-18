import { expect, test, type Page } from '@playwright/test'

async function login(page: Page): Promise<void> {
  await page.goto('/login')
  await page.getByLabel('Nome de usuário').fill('fagner')
  await page.locator('#login-password').fill('MinhaSenha123!')
  await page.getByRole('button', { name: 'Entrar na conta' }).click()
  await expect(page).toHaveURL('/')
}

test('alterna a preferência de notificar quando quem eu sigo publica', async ({ page }) => {
  await login(page)
  await page.goto('/dispositivos')
  await expect(page.getByRole('heading', { name: 'Meus dispositivos' })).toBeVisible()

  const checkbox = page.getByLabel('Me notificar quando alguém que eu sigo publicar')
  await expect(checkbox).toBeChecked()

  await checkbox.uncheck()
  await expect(checkbox).not.toBeChecked()

  await checkbox.check()
  await expect(checkbox).toBeChecked()
})
