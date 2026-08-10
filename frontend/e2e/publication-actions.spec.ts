import { expect, test, type Page } from '@playwright/test'

async function login(page: Page, username = 'fagner', password = 'MinhaSenha123!'): Promise<void> {
  await page.goto('/login')
  await page.getByLabel('Nome de usuário').fill(username)
  await page.locator('#login-password').fill(password)
  await page.getByRole('button', { name: 'Entrar na conta' }).click()
  await expect(page).toHaveURL('/')
}

test('card de receita vira por teclado e navega para detalhes', async ({ page }) => {
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

test('visitante vê as ações e retorna à publicação depois de entrar', async ({ page }) => {
  const publicationPath = '/publicacoes/7b0200b5-66e3-47b1-a80c-a2369379e1d3'
  await page.goto(publicationPath)

  await expect(page.getByRole('link', { name: /Eu comeria/ })).toBeVisible()
  await expect(
    page.getByRole('link', { name: 'Salvar publicação; é necessário entrar' }),
  ).toBeVisible()
  await page.getByRole('link', { name: /Eu comeria/ }).click()
  await expect(page).toHaveURL(/\/login\?redirect=/)

  await page.getByLabel('Nome de usuário').fill('fagner')
  await page.locator('#login-password').fill('MinhaSenha123!')
  await page.getByRole('button', { name: 'Entrar na conta' }).click()
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

  await expect(page.getByRole('button', { name: /Quero fazer/ })).toHaveAttribute(
    'aria-pressed',
    'true',
  )
  await expect(page.getByRole('button', { name: 'Remover dos salvos' })).toHaveText('Salvo')

  await page.getByRole('button', { name: /Eu comeria/ }).click()
  await expect(page.getByRole('button', { name: /Eu comeria/ })).toHaveAttribute(
    'aria-pressed',
    'true',
  )
  await page.getByRole('button', { name: 'Remover dos salvos' }).click()
  await page.getByRole('button', { name: 'Salvar publicação' }).click()
  await expect(page.getByRole('button', { name: 'Remover dos salvos' })).toBeVisible()

  await page.getByRole('button', { name: 'Denunciar' }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await page.getByRole('button', { name: 'Enviar denúncia' }).click()
  await expect(page.getByText('Denúncia enviada')).toBeVisible()

  await page.reload()
  await expect(page.getByRole('button', { name: /Eu comeria/ })).toHaveAttribute(
    'aria-pressed',
    'true',
  )
  await expect(page.getByRole('button', { name: 'Remover dos salvos' })).toHaveText('Salvo')
  await expect(page.getByText('Denúncia enviada')).toBeVisible()

  await page.getByRole('button', { name: /Fiz também/ }).click()
  await page.getByRole('link', { name: 'Cadastrar minha versão' }).click()
  await expect(page).toHaveURL(/\/publicar\/minha-versao\//)
})

test('estado personalizado não vaza ao trocar de usuário', async ({ page }) => {
  const publicationPath = '/publicacoes/7b0200b5-66e3-47b1-a80c-a2369379e1d3'
  await login(page)
  await page.goto(publicationPath)
  await expect(page.getByRole('button', { name: /Quero fazer/ })).toHaveAttribute(
    'aria-pressed',
    'true',
  )
  await expect(page.getByRole('button', { name: 'Remover dos salvos' })).toBeVisible()

  await page.getByRole('button', { name: 'Sair' }).click()
  await expect(page.getByRole('link', { name: 'Entrar' })).toBeVisible()
  await login(page, 'admin', 'AdminSenha123!')
  await page.goto(publicationPath)

  await expect(page.getByRole('button', { name: /Quero fazer/ })).toHaveAttribute(
    'aria-pressed',
    'false',
  )
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
