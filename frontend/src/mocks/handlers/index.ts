import { authMockHandlers } from './auth'
import { feedMockHandlers } from './feed'
import { discoveryMockHandlers } from './discovery'
import { moderationMockHandlers } from './moderation'
import { patchNotesMockHandlers } from './patch-notes'
import { getPublicationsMock } from '@/api/generated/publications/publications.msw'

export const mockHandlers = [
  ...authMockHandlers,
  ...discoveryMockHandlers,
  ...moderationMockHandlers,
  ...feedMockHandlers,
  ...patchNotesMockHandlers,
  ...getPublicationsMock(),
]
