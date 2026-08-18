import { authMockHandlers } from './auth'
import { biometricMockHandlers } from './biometric'
import { feedMockHandlers } from './feed'
import { discoveryMockHandlers } from './discovery'
import { moderationMockHandlers } from './moderation'
import { patchNotesMockHandlers } from './patch-notes'
import { getDevicesMock } from '@/api/generated/devices/devices.msw'
import { getPublicationsMock } from '@/api/generated/publications/publications.msw'

export const mockHandlers = [
  ...authMockHandlers,
  ...biometricMockHandlers,
  ...discoveryMockHandlers,
  ...moderationMockHandlers,
  ...feedMockHandlers,
  ...patchNotesMockHandlers,
  ...getDevicesMock(),
  ...getPublicationsMock(),
]
