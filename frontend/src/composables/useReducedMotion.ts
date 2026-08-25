import { storeToRefs } from 'pinia'

import { useReducedMotionStore } from '@/stores/reducedMotion.store'

export function useReducedMotion() {
  const reducedMotionStore = useReducedMotionStore()
  const { enabled } = storeToRefs(reducedMotionStore)

  return {
    enabled,
    setEnabled: reducedMotionStore.setEnabled,
  }
}
