import { expect, test, type Page } from '@playwright/test'

async function openAppRoute(page: Page, route: string): Promise<void> {
  await page.goto(route, { waitUntil: 'networkidle' })
  try {
    await expect(page.locator('main')).toBeVisible({ timeout: 15_000 })
  } catch {
    await page.reload({ waitUntil: 'networkidle' })
    await expect(page.locator('main')).toBeVisible({ timeout: 15_000 })
  }
}

async function login(page: Page, username = 'fagner', password = 'MinhaSenha123!'): Promise<void> {
  await page.goto('/login')
  await page.getByLabel('Nome de usuário').fill(username)
  await page.locator('#login-password').fill(password)
  await page.getByRole('button', { name: 'Entrar na conta' }).click()
  await expect(page).toHaveURL('/')
}

for (const theme of ['light', 'dark'] as const) {
  test(`rotas públicas mantêm composição visual no tema ${theme}`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1000 })
    await page.addInitScript(
      (selectedTheme) => window.localStorage.setItem('comes-e-bebes:theme', selectedTheme),
      theme,
    )
    for (const [route, name] of [
      ['/', 'feed'],
      ['/buscar', 'busca'],
      ['/u/fagner', 'perfil'],
      ['/publicacoes/7b0200b5-66e3-47b1-a80c-a2369379e1d3', 'detalhes'],
    ] as const) {
      await openAppRoute(page, route)
      if (route === '/') {
        await expect(page.locator('article')).toHaveCount(3)
        await expect
          .poll(() =>
            page
              .locator('main img')
              .evaluateAll((images) =>
                images.every((image) => image.complete && image.naturalWidth > 0),
              ),
          )
          .toBe(true)
      }
      await expect(page).toHaveScreenshot(`quality-${name}-${theme}.png`, {
        animations: 'disabled',
        fullPage: true,
      })
    }
  })

  test(`rotas autenticadas mantêm composição visual no tema ${theme}`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1000 })
    await page.addInitScript(
      (selectedTheme) => window.localStorage.setItem('comes-e-bebes:theme', selectedTheme),
      theme,
    )
    await login(page)
    for (const [route, name] of [
      ['/publicar', 'publicar'],
      ['/salvos', 'salvos'],
      ['/notificacoes', 'notificacoes'],
    ] as const) {
      await openAppRoute(page, route)
      await expect(page).toHaveScreenshot(`quality-${name}-${theme}.png`, {
        animations: 'disabled',
        fullPage: true,
      })
    }
  })
}

test('a interface mantém navegação por teclado e movimento reduzido', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await page.goto('/', { waitUntil: 'networkidle' })
  await expect(page.locator('article')).toHaveCount(3)
  const card = page.locator('.recipe-flip__front').first()
  await expect(card).toBeVisible()
  await card.focus()
  await expect(card).toBeFocused()
  await card.press('Space')
  await expect(page.locator('.recipe-flip--flipped')).toBeVisible()
  await expect(page.locator('.recipe-flip--flipped .recipe-flip__back')).toHaveCSS(
    'transform',
    'none',
  )
})

test('controles de formulário possuem nomes acessíveis', async ({ page }) => {
  await page.goto('/buscar')
  const unnamed = await page
    .locator('input, textarea, select, button')
    .evaluateAll(
      (controls) =>
        controls.filter(
          (control) =>
            !(
              (control as HTMLInputElement).labels?.length ||
              control.getAttribute('aria-label') ||
              control.textContent?.trim()
            ),
        ).length,
    )
  expect(unnamed).toBe(0)
})

test('rota administrativa mantém composição visual para ADMIN', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1000 })
  await login(page, 'admin', 'AdminSenha123!')
  await page.goto('/admin/moderacao')
  await expect(page.getByRole('heading', { name: 'Fila de moderação' })).toBeVisible()
  await expect(page).toHaveScreenshot('quality-moderation-light.png', {
    animations: 'disabled',
    fullPage: true,
  })
})

test('rotas principais não criam overflow horizontal em viewport mobile', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  for (const route of ['/', '/buscar', '/publicar']) {
    if (route === '/publicar') await login(page)
    await page.goto(route)
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - window.innerWidth,
    )
    expect(overflow).toBeLessThanOrEqual(1)
  }
})
