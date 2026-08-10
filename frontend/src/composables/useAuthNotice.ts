import { reactive } from 'vue'

export const authNotice = reactive({
  visible: false,
})

export function showAuthNotice(): void {
  authNotice.visible = true
}

export function dismissAuthNotice(): void {
  authNotice.visible = false
}
