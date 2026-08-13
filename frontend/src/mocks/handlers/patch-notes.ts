import { http, HttpResponse } from 'msw'

import { getPatchNotesMock } from '@/api/generated/patch-notes/patch-notes.msw'

export const patchNotesMockHandlers = [
  ...getPatchNotesMock(),
  http.patch('*/users/:id/patch-notes/seen', () => new HttpResponse(null, { status: 204 })),
]
