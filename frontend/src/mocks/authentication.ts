export function mockAuthenticatedUsername(request: Request): string | null {
  const authorization = request.headers.get('authorization')
  if (!authorization) return null

  return /^Bearer mock-access-([a-zA-Z0-9_]+)-/.exec(authorization)?.[1] ?? null
}
