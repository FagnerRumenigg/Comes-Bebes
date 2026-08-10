import { expect, test } from '@playwright/test'

test('login trata erro, autentica, restaura sessão e encerra', async ({ page }) => {
  await page.goto('/login')

  await page.getByLabel('Nome de usuário').fill('fagner')
  await page.locator('#login-password').fill('senha-incorreta')
  await page.getByRole('button', { name: 'Entrar na conta' }).click()
  await expect(page.getByRole('alert')).toContainText('Usuário ou senha inválidos.')
  await expect(page.getByLabel('Nome de usuário')).toHaveValue('fagner')

  await page.locator('#login-password').fill('MinhaSenha123!')
  await page.getByLabel('Lembrar-me neste dispositivo').check()
  await page.getByRole('button', { name: 'Entrar na conta' }).click()

  await expect(page).toHaveURL('/')
  await expect(page.getByRole('link', { name: '@fagner' })).toBeVisible()

  await page.reload()
  await expect(page.getByRole('link', { name: '@fagner' })).toBeVisible()

  await page.getByRole('button', { name: 'Sair' }).click()
  await expect(page.getByRole('link', { name: 'Entrar' })).toBeVisible()
})

test('cadastro valida, preserva os dados e permite entrar com a nova conta', async ({ page }) => {
  await page.goto('/cadastro')

  await page.getByLabel('Nome de exibição').fill('Cozinha de Teste')
  await page.getByLabel('Nome de usuário').fill('cozinha_nova')
  await page.locator('#register-password').fill('MinhaSenha123!')
  await page.locator('#register-confirm-password').fill('SenhaDiferente123!')
  await page.getByRole('button', { name: 'Criar conta' }).click()

  await expect(page.getByRole('alert')).toContainText('As senhas precisam ser iguais.')
  await expect(page.getByLabel('Nome de usuário')).toHaveValue('cozinha_nova')

  await page.locator('#register-confirm-password').fill('MinhaSenha123!')
  await page.getByRole('button', { name: 'Criar conta' }).click()

  await expect(page).toHaveURL(/\/login\?registered=true&username=cozinha_nova$/)
  await expect(page.getByText('Sua conta foi criada. Agora você já pode entrar.')).toBeVisible()
  await expect(page.getByLabel('Nome de usuário')).toHaveValue('cozinha_nova')

  await page.locator('#login-password').fill('MinhaSenha123!')
  await page.getByRole('button', { name: 'Entrar na conta' }).click()
  await expect(page.getByRole('link', { name: '@cozinha_nova' })).toBeVisible()
})

for (const theme of ['light', 'dark'] as const) {
  test(`login mantém a referência visual no tema ${theme}`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1000 })
    await page.addInitScript((selectedTheme) => {
      window.localStorage.setItem('comes-e-bebes:theme', selectedTheme)
    }, theme)
    await page.goto('/login', { waitUntil: 'networkidle' })
    await expect(page.getByRole('heading', { name: 'Bem-vindo de volta' })).toBeVisible({
      timeout: 20_000,
    })

    await expect(page).toHaveScreenshot(`login-${theme}.png`, {
      animations: 'disabled',
      fullPage: true,
    })
  })

  test(`cadastro mantém a referência visual no tema ${theme}`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1000 })
    await page.addInitScript((selectedTheme) => {
      window.localStorage.setItem('comes-e-bebes:theme', selectedTheme)
    }, theme)
    await page.goto('/cadastro', { waitUntil: 'networkidle' })
    await expect(page.getByRole('heading', { name: 'Crie sua conta' })).toBeVisible()

    await expect(page).toHaveScreenshot(`cadastro-${theme}.png`, {
      animations: 'disabled',
      fullPage: true,
    })
  })
}
