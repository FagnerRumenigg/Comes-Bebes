import { expect, test, type Page } from '@playwright/test'

async function login(page: Page): Promise<void> {
  await page.goto('/login')
  await page.getByLabel('Nome de usuário').fill('fagner')
  await page.locator('#login-password').fill('MinhaSenha123!')
  await page.getByRole('button', { name: 'Entrar na conta' }).click()
  await expect(page).toHaveURL('/')
}

const image = { name: 'prato.png', mimeType: 'image/png', buffer: Buffer.from('fake-image') }

test('criação de prato valida imagem e envia multipart', async ({ page }) => {
  await login(page)
  await page.goto('/publicar')

  await page.getByRole('button', { name: 'Publicar' }).click()
  await expect(page.getByRole('alert')).toContainText('Selecione uma imagem')

  await page.locator('input[type="file"]').setInputFiles({
    name: 'arquivo.txt',
    mimeType: 'text/plain',
    buffer: Buffer.from('não é uma imagem'),
  })
  await expect(page.getByRole('alert')).toContainText('JPEG, PNG, WebP, HEIC ou HEIF')

  await page.locator('input[type="file"]').setInputFiles({
    name: 'grande.jpg',
    mimeType: 'image/jpeg',
    buffer: Buffer.alloc(20 * 1024 * 1024 + 1),
  })
  await expect(page.getByRole('alert')).toContainText('no máximo 20 MB')

  await page.locator('input[type="file"]').setInputFiles(image)
  await page.getByLabel('Título').fill('Prato criado no teste')
  await expect(page.getByAltText('Prévia da imagem selecionada')).toBeVisible()
  await page.getByRole('button', { name: 'Publicar' }).click()
  await expect(page).toHaveURL(/\/publicacoes\//)
  await expect(page.getByRole('heading', { name: 'Prato criado no teste' })).toBeVisible()

  await page.reload()
  await expect(page.getByRole('heading', { name: 'Prato criado no teste' })).toBeVisible()
  await expect(page.locator('.publication-details__header')).toContainText('@fagner')
  await expect(page.getByRole('button', { name: /Eu comeria/ })).toBeDisabled()
  await expect(page.getByText('Você não pode reagir à própria publicação.')).toBeVisible()
  await expect(page.getByText('Sua publicação')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Denunciar' })).toHaveCount(0)
})

test('criação de receita exige ingrediente e preparo', async ({ page }) => {
  await login(page)
  await page.goto('/publicar')
  await page.getByLabel('Tipo de publicação').selectOption('RECIPE')
  await page.locator('input[type="file"]').setInputFiles(image)
  await page.getByRole('button', { name: 'Publicar' }).click()
  await expect(page.getByText('Adicione pelo menos um ingrediente.')).toBeVisible()

  await page.getByLabel('Ingrediente 1').fill('Farinha')
  await page.getByRole('button', { name: 'Publicar' }).click()
  await expect(page.getByText('Informe o modo de preparo.', { exact: true })).toBeVisible()
  await page.getByLabel('Modo de preparo').fill('Misture tudo.')
  await page.getByRole('button', { name: 'Publicar' }).click()
  await expect(page).toHaveURL(/\/publicacoes\//)
})

test('minha versão preserva a origem e pré-carrega a receita original', async ({ page }) => {
  await login(page)
  await page.goto('/publicar/minha-versao/7b0200b5-66e3-47b1-a80c-a2369379e1d3')
  await expect(page.getByRole('heading', { name: 'Publicar minha versão' })).toBeVisible()
  await expect(page.getByLabel('Ingrediente 1')).toHaveValue('cenoura')
  await page.getByLabel('Sufixo do título').fill('com cobertura cítrica')
  await page.locator('input[type="file"]').setInputFiles(image)
  await page.getByLabel('Modo de preparo').fill('Adapte a receita.')
  await page.getByRole('button', { name: 'Publicar' }).click()
  await expect(page).toHaveURL(/\/publicacoes\//)
})

test('minha versão interrompe o formulário quando a origem não existe', async ({ page }) => {
  await login(page)
  await page.goto('/publicar/minha-versao/00000000-0000-0000-0000-000000000000')

  await expect(page.getByRole('alert')).toContainText(
    'Não foi possível carregar a receita original.',
  )
  await expect(page.getByRole('button', { name: 'Publicar' })).toHaveCount(0)
})

test('rascunho é salvo ao sair, listado em Rascunhos e retomado para publicar', async ({
  page,
}) => {
  await login(page)
  await page.goto('/publicar')

  await page.getByLabel('Título').fill('Rascunho de bolo')
  await page.locator('input[type="file"]').setInputFiles(image)
  await expect(page.getByAltText('Prévia da imagem selecionada')).toBeVisible()

  // Navegação in-app (não page.goto, que recarrega a página inteira e nunca chega a
  // rodar onBeforeUnmount) — o autosave dispara ao desmontar o componente Vue.
  await page.getByRole('link', { name: 'Comes&Bebes — início' }).click()
  await expect(page).toHaveURL('/')

  await page.goto('/rascunhos')
  await expect(page.getByText('Rascunho de bolo')).toBeVisible()

  await page.getByRole('button', { name: 'Continuar editando' }).click()
  await expect(page).toHaveURL(/\/publicar\/rascunho\//)
  await expect(page.getByLabel('Título')).toHaveValue('Rascunho de bolo')
  await expect(page.getByAltText('Prévia da imagem selecionada')).toBeVisible()

  await page.getByRole('button', { name: 'Publicar' }).click()
  await expect(page).toHaveURL(/\/publicacoes\//)

  await page.goto('/rascunhos')
  await expect(page.getByText('Nenhum rascunho salvo.')).toBeVisible()
})
