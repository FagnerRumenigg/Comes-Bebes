import { expect, test, type Page } from '@playwright/test'

async function login(page: Page): Promise<void> {
  await page.goto('/login')
  await page.getByLabel('Nome de usuário').fill('fagner')
  await page.locator('#login-password').fill('MinhaSenha123!')
  await page.getByRole('button', { name: 'Entrar na conta' }).click()
  await expect(page).toHaveURL('/')
}

test('perfil público exibe identidade e publicações', async ({ page }) => {
  await page.goto('/u/fagner')
  await expect(page.getByRole('heading', { name: 'Fagner' })).toBeVisible()
  await expect(page.getByText('@fagner')).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Risoto de cogumelos' })).toBeVisible()
})

test('busca filtra por título e mostra estado vazio', async ({ page }) => {
  await page.goto('/buscar')
  await page.getByLabel('Título').fill('bolo')
  await page.getByRole('button', { name: 'Buscar' }).click()
  await expect(
    page.getByText('Bolo de cenoura com cobertura de chocolate', { exact: true }).first(),
  ).toBeVisible()
  await page.getByRole('button', { name: 'Limpar' }).click()
  await expect(page.getByText('Informe um título ou ingrediente para começar.')).toBeVisible()
})

test('segue e deixa de seguir outro usuário no perfil dele', async ({ page }) => {
  await login(page)
  await page.goto('/u/admin')
  await expect(page.getByRole('heading', { name: 'Administração' })).toBeVisible()

  const followButton = page.getByRole('button', { name: 'Seguir', exact: true })
  await expect(followButton).toBeVisible()
  await followButton.click()
  await expect(page.getByRole('button', { name: 'Deixar de seguir' })).toBeVisible()

  await page.reload()
  await expect(page.getByRole('button', { name: 'Deixar de seguir' })).toBeVisible()

  await page.getByRole('button', { name: 'Deixar de seguir' }).click()
  await expect(page.getByRole('button', { name: 'Seguir', exact: true })).toBeVisible()
})

test('não mostra botão de seguir no próprio perfil', async ({ page }) => {
  await login(page)
  await page.goto('/u/fagner')
  await expect(page.getByRole('heading', { name: 'Fagner' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Seguir', exact: true })).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Deixar de seguir' })).toHaveCount(0)
})

test('usuário autenticado vê salvos e notificações', async ({ page }) => {
  await login(page)
  await page.goto('/salvos')
  await expect(page.getByRole('heading', { name: 'Salvos' })).toBeVisible()
  await expect(
    page.getByText('Bolo de cenoura com cobertura de chocolate', { exact: true }).first(),
  ).toBeVisible()

  await page.goto('/notificacoes')
  await expect(page.getByRole('heading', { name: 'Notificações' })).toBeVisible()
  await expect(page.getByText('Sua publicação foi aprovada.')).toBeVisible()
  await expect(page.getByText('Não lida')).toBeVisible()
})
