import { delay, http, HttpResponse } from 'msw'

import { searchMockTags } from '@/mocks/state/tags'

export const tagsMockHandlers = [
  http.get('*/tags/search', async ({ request }) => {
    await delay(150)
    const query = new URL(request.url).searchParams.get('q') ?? ''
    return HttpResponse.json(searchMockTags(query))
  }),
]
