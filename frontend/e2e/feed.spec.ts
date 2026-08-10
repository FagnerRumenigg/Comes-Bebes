import { expect, test } from '@playwright/test'

test('feed público exibe somente publicações públicas', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('heading', { name: 'Seu feed' })).toBeVisible()
  await expect(page.locator('article')).toHaveCount(3)
  await expect(page.getByText('Somente comunidade')).toHaveCount(0)
  await expect(page.getByText('Você chegou ao fim por hoje.')).toBeVisible()
})

test('feed autenticado inclui publicações internas', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('Nome de usuário').fill('fagner')
  await page.locator('#login-password').fill('MinhaSenha123!')
  await page.getByRole('button', { name: 'Entrar na conta' }).click()

  await expect(page).toHaveURL('/')
  await expect(page.locator('article')).toHaveCount(4)
  await expect(page.getByText('Somente comunidade')).toBeVisible()

  await page.getByRole('button', { name: 'Sair' }).click()
  await expect(page.locator('article')).toHaveCount(3)
  await expect(page.getByText('Somente comunidade')).toHaveCount(0)
})

for (const theme of ['light', 'dark'] as const) {
  test(`feed mantém a referência visual no tema ${theme}`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1000 })
    await page.addInitScript((selectedTheme) => {
      window.localStorage.setItem('comes-e-bebes:theme', selectedTheme)
    }, theme)
    await page.goto('/')
    await expect(page.locator('article')).toHaveCount(3)

    await page.locator('article').last().scrollIntoViewIfNeeded()
    await expect(page.locator('main img')).toHaveCount(3)
    await expect
      .poll(() =>
        page
          .locator('main img')
          .evaluateAll((images) =>
            images.every((image) => image.complete && image.naturalWidth > 0),
          ),
      )
      .toBe(true)
    await page.getByRole('heading', { name: 'Seu feed' }).scrollIntoViewIfNeeded()

    await expect(page).toHaveScreenshot(`feed-${theme}.png`, {
      animations: 'disabled',
      fullPage: true,
    })
  })
}
