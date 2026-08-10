import { expect, test, type Page } from '@playwright/test'

async function login(page: Page, username: string, password: string): Promise<void> {
  await page.goto('/login')
  await page.getByLabel('Nome de usuário').fill(username)
  await page.locator('#login-password').fill(password)
  await page.getByRole('button', { name: 'Entrar na conta' }).click()
  await expect(page).toHaveURL('/')
}

test('usuário comum não acessa a moderação', async ({ page }) => {
  await login(page, 'fagner', 'MinhaSenha123!')
  await page.goto('/admin/moderacao')
  await expect(page).toHaveURL('/')
})

test('administrador analisa caso e exige justificativa para ocultar', async ({ page }) => {
  await login(page, 'admin', 'AdminSenha123!')
  await page.goto('/admin/moderacao')
  await expect(page.getByRole('heading', { name: 'Fila de moderação' })).toBeVisible()
  await page.getByRole('link', { name: 'Analisar caso' }).first().click()
  await expect(page.getByRole('heading', { name: 'Análise da publicação' })).toBeVisible()
  await page.getByLabel('Decisão').selectOption('HIDDEN')
  await page.getByRole('button', { name: 'Registrar decisão' }).click()
  await expect(page.getByRole('alert')).toContainText('Justificativa obrigatória')
  await page
    .getByLabel('Justificativa')
    .fill('Conteúdo analisado conforme as regras da comunidade.')
  await page.getByRole('button', { name: 'Registrar decisão' }).click()
  await expect(page).toHaveURL('/admin/moderacao')
})
