import { reactive } from 'vue'

export const authNotice = reactive({
  visible: false,
})

let dismissalTimer: ReturnType<typeof setTimeout> | undefined

export function showAuthNotice(): void {
  if (dismissalTimer) clearTimeout(dismissalTimer)
  authNotice.visible = true
  dismissalTimer = setTimeout(() => {
    authNotice.visible = false
    dismissalTimer = undefined
  }, 2_000)
}

export function dismissAuthNotice(): void {
  if (dismissalTimer) clearTimeout(dismissalTimer)
  dismissalTimer = undefined
  authNotice.visible = false
}
