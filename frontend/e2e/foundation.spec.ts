import { expect, test } from '@playwright/test'

test('a aplicação navega pela fundação e protege rotas autenticadas', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('heading', { name: 'Feed' })).toBeVisible()
  await expect(page.getByRole('banner')).toBeVisible()

  await page.getByRole('link', { name: 'Buscar', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Buscar' })).toBeVisible()

  await page.goto('/publicar')
  await expect(page).toHaveURL(/\/login\?redirect=\/publicar$/)
  await expect(page.getByRole('heading', { name: 'Bem-vindo de volta' })).toBeVisible()
})

test('a preferência de tema permanece após recarregar', async ({ page }) => {
  await page.goto('/')

  await page.getByRole('button', { name: 'Ativar tema escuro' }).click()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')

  await page.reload()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')

  const darkPalette = await page.locator('html').evaluate((element) => {
    const styles = getComputedStyle(element)
    return {
      background: styles.getPropertyValue('--color-background').trim(),
      surface: styles.getPropertyValue('--color-surface').trim(),
      primary: styles.getPropertyValue('--color-primary').trim(),
      accent: styles.getPropertyValue('--color-accent').trim(),
    }
  })
  expect(darkPalette).toEqual({
    background: '#2d1a11',
    surface: '#341e14',
    primary: '#e99d7a',
    accent: '#b5cc79',
  })
})

test('a navegação móvel expõe os cinco destinos previstos', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/')

  const navigation = page.getByRole('navigation', { name: 'Navegação móvel' })
  await expect(navigation).toBeVisible()
  await expect(navigation.getByRole('link')).toHaveCount(5)
})
