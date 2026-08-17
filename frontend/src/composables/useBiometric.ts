import {
  completeAuthentication,
  completeRegistration,
  startAuthentication,
  startRegistration,
  status as fetchStatus,
} from '@/api/generated/biometric/biometric'
import type {
  BiometricResponse,
  CompleteBiometricRegistrationRequestBiometricType,
  JsonNode,
  LoginResponse,
} from '@/api/generated/models'

export function isWebAuthnSupported(): boolean {
  return typeof window !== 'undefined' && typeof window.PublicKeyCredential === 'function'
}

export async function isPlatformAuthenticatorAvailable(): Promise<boolean> {
  if (!isWebAuthnSupported()) return false
  try {
    return await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable()
  } catch {
    return false
  }
}

/** Rótulo de exibição só — o backend não distingue tipos, é so texto. */
export function detectBiometricLabel(): CompleteBiometricRegistrationRequestBiometricType {
  if (typeof navigator === 'undefined') return 'UNKNOWN'
  const ua = navigator.userAgent
  if (/iPhone|iPad|iPod|Macintosh/.test(ua)) {
    return /iPhone|iPad|iPod/.test(ua) ? 'FACE_ID' : 'FINGERPRINT'
  }
  if (/Windows/.test(ua)) return 'WINDOWS_HELLO'
  if (/Android/.test(ua)) return 'FINGERPRINT'
  return 'UNKNOWN'
}

export function useBiometric() {
  async function checkStatus(deviceId: string): Promise<boolean> {
    const response = await fetchStatus({ deviceId })
    return response.hasBiometric
  }

  async function register(deviceId: string): Promise<BiometricResponse> {
    const startResponse = await startRegistration({ deviceId })
    const creationOptions = PublicKeyCredential.parseCreationOptionsFromJSON(
      startResponse.publicKeyCredentialCreationOptions as unknown as PublicKeyCredentialCreationOptionsJSON,
    )
    const credential = await navigator.credentials.create({ publicKey: creationOptions })
    if (!(credential instanceof PublicKeyCredential)) {
      throw new Error('Não foi possível criar a credencial biométrica.')
    }

    return completeRegistration({
      deviceId,
      state: startResponse.state,
      credential: credential.toJSON() as unknown as JsonNode,
      biometricType: detectBiometricLabel(),
    })
  }

  async function authenticate(deviceId: string): Promise<LoginResponse> {
    const startResponse = await startAuthentication({ deviceId })
    const requestOptions = PublicKeyCredential.parseRequestOptionsFromJSON(
      startResponse.publicKeyCredentialRequestOptions as unknown as PublicKeyCredentialRequestOptionsJSON,
    )
    const credential = await navigator.credentials.get({ publicKey: requestOptions })
    if (!(credential instanceof PublicKeyCredential)) {
      throw new Error('Não foi possível obter a credencial biométrica.')
    }

    return completeAuthentication({
      deviceId,
      state: startResponse.state,
      credential: credential.toJSON() as unknown as JsonNode,
    })
  }

  return { checkStatus, register, authenticate }
}
