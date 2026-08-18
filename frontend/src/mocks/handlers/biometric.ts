import { HttpResponse, http } from 'msw'

import {
  getCompleteAuthenticationMockHandler,
  getCompleteRegistrationMockHandler,
  getList1MockHandler,
  getRemoveMockHandler,
  getStartAuthenticationMockHandler,
  getStartRegistrationMockHandler,
} from '@/api/generated/biometric/biometric.msw'
import type { BiometricStatusResponse } from '@/api/generated/models'

export const biometricMockHandlers = [
  // "Já registrada" por padrão nos mocks: em máquinas com autenticador de plataforma
  // real (ex.: Windows Hello no host dos testes), isPlatformAuthenticatorAvailable()
  // retorna true de verdade, e sem isso o prompt "Ativar biometria?" apareceria depois
  // de qualquer login nos testes e2e que não têm nada a ver com biometria. Testes que
  // queiram exercitar o prompt devem sobrescrever este handler com hasBiometric: false.
  http.get('*/auth/biometric/status', () =>
    HttpResponse.json<BiometricStatusResponse>({ hasBiometric: true }),
  ),
  getStartRegistrationMockHandler(),
  getCompleteRegistrationMockHandler(),
  getStartAuthenticationMockHandler(),
  getCompleteAuthenticationMockHandler(),
  getList1MockHandler([]),
  getRemoveMockHandler(),
]
