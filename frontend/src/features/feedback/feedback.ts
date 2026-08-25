import { useMutation } from '@tanstack/vue-query'

import { apiRequest } from '@/api/client'

/** POST /feedback é endpoint novo, ainda sem passar pelo orval. */
export interface SubmitFeedbackPayload {
  message: string
  contactEmail?: string
}

export function useSubmitFeedback() {
  return useMutation({
    mutationFn: (payload: SubmitFeedbackPayload) =>
      apiRequest<void>({ url: '/feedback', method: 'POST', data: payload }),
  })
}
