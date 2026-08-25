import { authMockHandlers } from './auth'
import { biometricMockHandlers } from './biometric'
import { collectionsMockHandlers } from './collections'
import { feedMockHandlers } from './feed'
import { discoveryMockHandlers } from './discovery'
import { followMockHandlers } from './follow'
import { moderationMockHandlers } from './moderation'
import { notificationPreferencesMockHandlers } from './notification-preferences'
import { patchNotesMockHandlers } from './patch-notes'
import { tagsMockHandlers } from './tags'
import { usersMockHandlers } from './users'
import { getDevicesMock } from '@/api/generated/devices/devices.msw'
import { getPublicationsMock } from '@/api/generated/publications/publications.msw'

export const mockHandlers = [
  ...authMockHandlers,
  ...biometricMockHandlers,
  ...collectionsMockHandlers,
  ...discoveryMockHandlers,
  ...followMockHandlers,
  ...moderationMockHandlers,
  ...notificationPreferencesMockHandlers,
  ...feedMockHandlers,
  ...patchNotesMockHandlers,
  ...tagsMockHandlers,
  ...usersMockHandlers,
  ...getDevicesMock(),
  ...getPublicationsMock(),
]
