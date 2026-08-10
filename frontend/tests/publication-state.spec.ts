import { describe, expect, it } from 'vitest'

import { mockPublications } from '@/mocks/fixtures/publications'
import {
  personalizePublication,
  reportPublication,
  savedPublications,
  setPublicationReaction,
  setPublicationSaved,
} from '@/mocks/state/publications'

describe('estado personalizado das publicações', () => {
  it('isola salvos, reações e denúncias por usuário', () => {
    const publication = mockPublications[2]!
    const firstUser = 'isolated_user_one'
    const secondUser = 'isolated_user_two'

    setPublicationSaved(firstUser, publication.id, true)
    setPublicationReaction(firstUser, publication.id, 'COMFORT_FOOD', true)
    expect(reportPublication(firstUser, publication.id)).toBe(true)
    expect(reportPublication(firstUser, publication.id)).toBe(false)

    expect(personalizePublication(publication, firstUser)).toMatchObject({
      saved: true,
      selectedReactions: ['COMFORT_FOOD'],
      reportedByCurrentUser: true,
    })
    expect(personalizePublication(publication, secondUser)).toMatchObject({
      saved: false,
      selectedReactions: [],
      reportedByCurrentUser: false,
    })
    expect(savedPublications(firstUser)).toEqual([
      expect.objectContaining({ publicationId: publication.id }),
    ])
    expect(savedPublications(secondUser)).toEqual([])
  })
})
