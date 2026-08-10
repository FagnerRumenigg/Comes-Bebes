import { defineConfig } from 'orval'

export default defineConfig({
  comesEBebes: {
    input: {
      target: './openapi/openapi.json',
    },
    output: {
      mode: 'tags-split',
      target: './src/api/generated/endpoints.ts',
      schemas: './src/api/generated/models',
      client: 'vue-query',
      mock: true,
      clean: true,
      override: {
        mutator: {
          path: './src/api/client.ts',
          name: 'apiRequest',
        },
      },
    },
  },
})
