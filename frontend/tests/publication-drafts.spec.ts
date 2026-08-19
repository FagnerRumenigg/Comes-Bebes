import 'fake-indexeddb/auto'
import { afterEach, describe, expect, it } from 'vitest'

import {
  deleteDraft,
  getDraft,
  hasDraftContent,
  listDrafts,
  saveDraft,
  type PublicationDraft,
} from '@/features/publications/drafts'

function makeDraft(overrides: Partial<PublicationDraft> = {}): PublicationDraft {
  const now = new Date().toISOString()
  return {
    id: crypto.randomUUID(),
    mode: 'CREATE',
    sourceId: null,
    createdAt: now,
    updatedAt: now,
    type: 'DISH',
    visibility: 'PUBLIC',
    title: '',
    description: '',
    titleSuffix: '',
    changeSummary: '',
    instructions: '',
    yieldQuantity: '',
    yieldUnit: '',
    ingredients: [],
    image: null,
    tags: [],
    ...overrides,
  }
}

async function clearAllDrafts(): Promise<void> {
  for (const draft of await listDrafts()) await deleteDraft(draft.id)
}

describe('publication drafts (IndexedDB)', () => {
  afterEach(async () => {
    await clearAllDrafts()
  })

  it('salva e recupera um rascunho pelo id', async () => {
    const draft = makeDraft({ title: 'Bolo de cenoura' })

    await saveDraft(draft)
    const loaded = await getDraft(draft.id)

    expect(loaded?.title).toBe('Bolo de cenoura')
  })

  it('retorna null para um rascunho inexistente', async () => {
    expect(await getDraft('nao-existe')).toBeNull()
  })

  it('lista rascunhos do mais recente para o mais antigo', async () => {
    const older = makeDraft({ title: 'Mais antigo', updatedAt: '2026-01-01T00:00:00.000Z' })
    const newer = makeDraft({ title: 'Mais recente', updatedAt: '2026-02-01T00:00:00.000Z' })

    await saveDraft(older)
    await saveDraft(newer)
    const drafts = await listDrafts()

    expect(drafts.map((draft) => draft.title)).toEqual(['Mais recente', 'Mais antigo'])
  })

  it('substitui o rascunho existente ao salvar com o mesmo id (upsert)', async () => {
    const draft = makeDraft({ title: 'Versão 1' })
    await saveDraft(draft)
    await saveDraft({ ...draft, title: 'Versão 2' })

    const drafts = await listDrafts()

    expect(drafts).toHaveLength(1)
    expect(drafts[0]?.title).toBe('Versão 2')
  })

  it('remove um rascunho', async () => {
    const draft = makeDraft()
    await saveDraft(draft)

    await deleteDraft(draft.id)

    expect(await getDraft(draft.id)).toBeNull()
  })

  it('considera vazio um rascunho sem nenhum campo preenchido', () => {
    expect(hasDraftContent(makeDraft())).toBe(false)
  })

  it.each([
    ['title', { title: 'Algo' }],
    ['description', { description: 'Algo' }],
    ['titleSuffix', { titleSuffix: 'Algo' }],
    ['changeSummary', { changeSummary: 'Algo' }],
    ['instructions', { instructions: 'Algo' }],
  ])('considera com conteúdo um rascunho com %s preenchido', (_field, overrides) => {
    expect(hasDraftContent(makeDraft(overrides))).toBe(true)
  })

  it('considera com conteúdo um rascunho com ingrediente nomeado', () => {
    const draft = makeDraft({
      ingredients: [{ name: 'Farinha', quantity: '', unit: '', note: '' }],
    })
    expect(hasDraftContent(draft)).toBe(true)
  })

  it('considera com conteúdo um rascunho com imagem anexada', () => {
    const draft = makeDraft({ image: new File(['x'], 'foto.jpg', { type: 'image/jpeg' }) })
    expect(hasDraftContent(draft)).toBe(true)
  })
})
