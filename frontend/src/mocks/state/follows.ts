const MOCK_STATE_STORAGE_KEY = 'comes-e-bebes:mocks:follow-state'

const followingByUser = new Map<string, Set<string>>()

interface SerializedFollowState {
  following: Array<[string, string[]]>
}

function restoreState(): void {
  if (typeof window === 'undefined') return
  try {
    const value = window.localStorage.getItem(MOCK_STATE_STORAGE_KEY)
    if (!value) return
    const state = JSON.parse(value) as SerializedFollowState
    if (!Array.isArray(state.following)) throw new Error('Estado mockado incompatível.')
    followingByUser.clear()
    for (const [username, ids] of state.following) {
      followingByUser.set(username, new Set(ids))
    }
  } catch {
    window.localStorage.removeItem(MOCK_STATE_STORAGE_KEY)
  }
}

function persistState(): void {
  if (typeof window === 'undefined') return
  const state: SerializedFollowState = {
    following: [...followingByUser].map(([username, ids]) => [username, [...ids]]),
  }
  window.localStorage.setItem(MOCK_STATE_STORAGE_KEY, JSON.stringify(state))
}

restoreState()

function followingFor(username: string): Set<string> {
  const existing = followingByUser.get(username)
  if (existing) return existing
  const created = new Set<string>()
  followingByUser.set(username, created)
  return created
}

export function isFollowingMock(username: string, targetUserId: string): boolean {
  return followingFor(username).has(targetUserId)
}

export function setFollowingMock(username: string, targetUserId: string, following: boolean): void {
  const set = followingFor(username)
  if (following) set.add(targetUserId)
  else set.delete(targetUserId)
  persistState()
}

export function followersCountMock(targetUserId: string): number {
  let count = 0
  for (const set of followingByUser.values()) {
    if (set.has(targetUserId)) count += 1
  }
  return count
}

export function followingCountMock(username: string): number {
  return followingFor(username).size
}
