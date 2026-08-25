import { expect, test, type Page } from '@playwright/test'

async function login(page: Page, username = 'fagner', password = 'MinhaSenha123!'): Promise<void> {
  await page.goto('/login')
  await page.getByLabel('E-mail ou usuário').fill(username)
  await page.locator('#login-password').fill(password)
  await page.getByRole('button', { name: 'Entrar na conta' }).click()
  await expect(page).toHaveURL('/')
}

async function logout(page: Page): Promise<void> {
  await page.locator('.account-menu__trigger--web').click()
  await page.getByRole('button', { name: 'Sair da conta' }).click()
}

test('card de receita vira por teclado e navega para detalhes', async ({ page }) => {
  await page.addInitScript(() =>
    window.localStorage.setItem('comes-e-bebes:welcome-seen', 'true'),
  )
  await page.goto('/', { waitUntil: 'networkidle' })
  await expect(page.locator('article')).toHaveCount(3)
  const flip = page.locator('.recipe-flip__front').first()
  await expect(flip).toBeVisible()
  await flip.focus()
  await flip.press('Enter')
  const backButton = page
    .locator('.recipe-flip--flipped')
    .getByRole('button', { name: 'Voltar para a foto' })
  await expect(backButton).toBeVisible()
  await backButton.click()
  await expect(page.locator('.recipe-flip--flipped')).toHaveCount(0)
  await expect(flip).toHaveAttribute('aria-disabled', 'false')
  await flip.press('Enter')
  await expect(page.locator('.recipe-flip--flipped .recipe-flip__link')).toBeVisible()
  const detailsLink = page.locator('.recipe-flip--flipped .recipe-flip__link')
  await expect(detailsLink).toHaveAttribute('href', /\/publicacoes\//)
  await detailsLink.evaluate((element) => (element as HTMLAnchorElement).click())
  await expect(page).toHaveURL(/\/publicacoes\//)
  await expect(
    page.getByRole('heading', { name: 'Bolo de cenoura com cobertura de chocolate' }),
  ).toBeVisible()
})

test('visitante vê as ações e recebe um aviso ao tentar reagir', async ({ page }) => {
  const publicationPath = '/publicacoes/7b0200b5-66e3-47b1-a80c-a2369379e1d3'
  await page.goto(publicationPath)

  await expect(page.getByRole('button', { name: 'Reagir' })).toBeVisible()
  await expect(
    page.getByRole('button', { name: 'Salvar publicação; é necessário entrar' }),
  ).toBeVisible()

  // Ações dentro da página (Reagir/Salvar) não expulsam o visitante para o
  // login — só avisam (showAuthNotice). Só rotas inteiras (ex.: /publicar)
  // exigem entrar antes de abrir, ver foundation.spec.ts.
  await page.getByRole('button', { name: 'Reagir' }).click()
  await expect(page.getByText('Você precisa estar conectado para realizar essa ação.')).toBeVisible()
  await expect(page).toHaveURL(publicationPath)
})

test('detalhe permite reagir, salvar, denunciar e iniciar minha versão', async ({ page }) => {
  await login(page)
  await page.goto('/publicacoes/7b0200b5-66e3-47b1-a80c-a2369379e1d3')
  await expect(
    page.getByRole('heading', { name: 'Bolo de cenoura com cobertura de chocolate' }),
  ).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Ingredientes' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Modo de preparo' })).toBeVisible()

  // "fagner" já tinha reagido com "Me deu fome" nesta publicação (estado mockado).
  await page.getByRole('button', { name: 'Reagir' }).click()
  await expect(page.getByRole('button', { name: 'Me deu fome' })).toHaveAttribute(
    'aria-pressed',
    'true',
  )
  await page.getByRole('button', { name: 'Ficou lindo' }).click()
  await expect(page.getByRole('button', { name: 'Ficou lindo' })).toHaveAttribute(
    'aria-pressed',
    'true',
  )
  await page.keyboard.press('Escape')
  await expect(page.getByRole('button', { name: 'Organizar nos salvos' })).toHaveText('Guardado')

  // Fluxo fundido (docs/telas/05-feed.html): tirar dos salvos abre a folha de
  // organizar; salvar de novo é 1 toque só, com toast de confirmação.
  await page.getByRole('button', { name: 'Organizar nos salvos' }).click()
  await page.getByRole('button', { name: 'Tirar dos salvos' }).click()
  await expect(page.getByRole('button', { name: 'Salvar publicação' })).toBeVisible()
  await page.getByRole('button', { name: 'Salvar publicação' }).click()
  await expect(page.getByText('Guardado nos seus salvos.')).toBeVisible()
  await page.getByRole('button', { name: 'Dispensar' }).click()
  await expect(page.getByRole('button', { name: 'Organizar nos salvos' })).toBeVisible()

  await page.getByRole('button', { name: 'Denunciar' }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await page.getByRole('button', { name: 'Enviar denúncia' }).click()
  await expect(page.getByText('Denúncia enviada')).toBeVisible()

  await page.reload()
  // A reação marcada antes do reload passa a existir no servidor: reaparece como
  // emblema no cartão, sem número (produto5.md v5 §3.1). Escopado ao emblema
  // porque o diálogo do seletor de reações (fechado) continua no DOM e tem
  // um botão com o mesmo texto.
  await expect(page.locator('.reaction-bar__badge', { hasText: 'Ficou lindo' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Organizar nos salvos' })).toHaveText('Guardado')
  await expect(page.getByText('Denúncia enviada')).toBeVisible()

  await page.getByRole('button', { name: /Publicar minha versão/ }).click()
  await page.getByRole('link', { name: 'Cadastrar minha versão' }).click()
  await expect(page).toHaveURL(/\/publicar\/minha-versao\//)
})

test('estado personalizado não vaza ao trocar de usuário', async ({ page }) => {
  // Depois do logout o app cai no feed como visitante; sem esta flag, o
  // gate de primeira visita (routeAccessGuard) redireciona para /bem-vindo
  // (tela sem o header com o link "Entrar", ver 01-boas-vindas-e-erro.html).
  await page.addInitScript(() =>
    window.localStorage.setItem('comes-e-bebes:welcome-seen', 'true'),
  )
  const publicationPath = '/publicacoes/7b0200b5-66e3-47b1-a80c-a2369379e1d3'
  await login(page)
  await page.goto(publicationPath)
  await page.getByRole('button', { name: 'Reagir' }).click()
  await expect(page.getByRole('button', { name: 'Me deu fome' })).toHaveAttribute(
    'aria-pressed',
    'true',
  )
  await page.keyboard.press('Escape')
  await expect(page.getByRole('button', { name: 'Organizar nos salvos' })).toBeVisible()

  await logout(page)
  await expect(page.getByRole('link', { name: 'Entrar' })).toBeVisible()
  await login(page, 'admin', 'AdminSenha123!')
  await page.goto(publicationPath)

  await page.getByRole('button', { name: 'Reagir' }).click()
  await expect(page.getByRole('button', { name: 'Me deu fome' })).toHaveAttribute(
    'aria-pressed',
    'false',
  )
  await page.keyboard.press('Escape')
  await expect(page.getByRole('button', { name: 'Salvar publicação' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Denunciar' })).toBeVisible()
})

for (const theme of ['light', 'dark'] as const) {
  test(`detalhes da receita mantêm a referência visual no tema ${theme}`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1000 })
    await page.addInitScript((selectedTheme) => {
      window.localStorage.setItem('comes-e-bebes:theme', selectedTheme)
    }, theme)
    await page.goto('/publicacoes/7b0200b5-66e3-47b1-a80c-a2369379e1d3')
    await expect(
      page.getByRole('heading', { name: 'Bolo de cenoura com cobertura de chocolate' }),
    ).toBeVisible()
    await expect(page).toHaveScreenshot(`publication-details-${theme}.png`, {
      animations: 'disabled',
      fullPage: true,
    })
  })
}
