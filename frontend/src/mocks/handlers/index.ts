import { authMockHandlers } from './auth'
import { feedMockHandlers } from './feed'
import { discoveryMockHandlers } from './discovery'
import { moderationMockHandlers } from './moderation'
import { getPublicationsMock } from '@/api/generated/publications/publications.msw'

export const mockHandlers = [
  ...authMockHandlers,
  ...discoveryMockHandlers,
  ...moderationMockHandlers,
  ...feedMockHandlers,
  ...getPublicationsMock(),
]
