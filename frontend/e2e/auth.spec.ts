import { expect, test } from '@playwright/test'

test('login trata erro, autentica, restaura sessão e encerra', async ({ page }) => {
  // Depois do logout o app cai no feed como visitante; sem esta flag, o
  // gate de primeira visita (routeAccessGuard) redireciona para /bem-vindo
  // (tela sem o header com o link "Entrar", ver 01-boas-vindas-e-erro.html).
  await page.addInitScript(() =>
    window.localStorage.setItem('comes-e-bebes:welcome-seen', 'true'),
  )
  await page.goto('/login')

  await page.getByLabel('E-mail ou usuário').fill('fagner')
  await page.locator('#login-password').fill('senha-incorreta')
  await page.getByRole('button', { name: 'Entrar na conta' }).click()
  await expect(page.getByRole('alert')).toContainText('E-mail, usuário ou senha inválidos.')
  await expect(page.getByLabel('E-mail ou usuário')).toHaveValue('fagner')

  await page.locator('#login-password').fill('MinhaSenha123!')
  await page.getByLabel('Lembrar-me neste dispositivo').check()
  await page.getByRole('button', { name: 'Entrar na conta' }).click()

  await expect(page).toHaveURL('/')
  await expect(page.getByRole('button', { name: '@fagner' })).toBeVisible()

  await page.reload()
  await expect(page.getByRole('button', { name: '@fagner' })).toBeVisible()

  await page.locator('.account-menu__trigger--web').click()
  await page.getByRole('button', { name: 'Sair da conta' }).click()
  await expect(page.getByRole('link', { name: 'Entrar' })).toBeVisible()
})

test('cadastro valida, mostra a tela de sucesso e permite entrar com a nova conta', async ({
  page,
}) => {
  await page.goto('/cadastro')

  await page.getByLabel('Como você quer ser chamado?').fill('Cozinha de Teste')
  await page.getByLabel('E-mail').fill('cozinha_nova@exemplo.com.br')
  await page.locator('#register-password').fill('MinhaSenha123!')
  await page.locator('#register-confirm-password').fill('SenhaDiferente123!')
  await page.getByRole('button', { name: 'Criar conta' }).click()

  await expect(page.getByRole('alert')).toContainText('As senhas precisam ser iguais.')
  await expect(page.getByLabel('E-mail')).toHaveValue('cozinha_nova@exemplo.com.br')

  await page.locator('#register-confirm-password').fill('MinhaSenha123!')
  await page.getByRole('button', { name: 'Criar conta' }).click()

  // Cadastro fica na mesma tela (docs/telas/02-login-e-cadastro.html #done) — não
  // redireciona mais para /login.
  await expect(page.getByRole('heading', { name: /^Pronto, Cozinha de Teste!$/ })).toBeVisible()
  await expect(page.getByText('cozinha_nova@exemplo.com.br')).toBeVisible()

  await page.getByRole('button', { name: 'Começar a explorar' }).click()
  await expect(page).toHaveURL('/')
  await expect(page.getByRole('link', { name: 'Entrar' })).toBeVisible()

  // Navegação in-app (não page.goto, que recarrega a página inteira e perde
  // a conta recém-criada — ela só existe na memória do mock desta sessão).
  await page.getByRole('link', { name: 'Entrar' }).click()
  await expect(page).toHaveURL('/login')
  await page.getByLabel('E-mail ou usuário').fill('cozinha_nova@exemplo.com.br')
  await page.locator('#login-password').fill('MinhaSenha123!')
  await page.getByRole('button', { name: 'Entrar na conta' }).click()

  // Conta recém-criada ainda não concluiu o onboarding — cai na mesma tela
  // de boas-vindas (docs/telas/01), não numa tela de onboarding separada.
  await expect(page).toHaveURL('/bem-vindo')
  await page.getByRole('button', { name: 'Começar a explorar' }).click()
  await expect(page).toHaveURL('/')
  await expect(page.getByRole('button', { name: '@cozinha_de_teste' })).toBeVisible()
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
