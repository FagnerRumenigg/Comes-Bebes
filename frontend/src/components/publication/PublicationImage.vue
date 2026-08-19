<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = defineProps<{
  src: string
  alt: string
}>()

const failed = ref(false)
const reloadKey = ref(0)
const backendBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8082'

function resolveBackendAsset(path: string): string {
  const joinedPath = `${backendBaseUrl.replace(/\/$/, '')}/${path.replace(/^\//, '')}`
  return new URL(joinedPath, window.location.origin).toString()
}

const resolvedSrc = computed(() => {
  if (
    /^(?:https?:\/\/|data:|blob:)/i.test(props.src) ||
    props.src.startsWith('/src/') ||
    props.src.startsWith('/assets/')
  ) {
    return props.src
  }
  return resolveBackendAsset(props.src)
})

watch(
  () => props.src,
  () => {
    failed.value = false
    reloadKey.value += 1
  },
)

function retry(): void {
  failed.value = false
  reloadKey.value += 1
}
</script>

<template>
  <div class="publication-image">
    <img
      v-if="!failed"
      :key="reloadKey"
      :src="resolvedSrc"
      :alt="alt"
      loading="eager"
      @error="failed = true"
    />
    <div v-else class="publication-image__fallback" role="status">
      <span aria-hidden="true">◇</span>
      <strong>A imagem não está mais disponível.</strong>
      <p>A URL pode ter expirado. Tente carregá-la novamente.</p>
      <button type="button" @click="retry">Tentar novamente</button>
    </div>
  </div>
</template>

<style scoped>
.publication-image {
  display: grid;
  overflow: hidden;
  aspect-ratio: 4 / 5;
  background: color-mix(in srgb, var(--color-border) 38%, var(--color-surface));
}

.publication-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.publication-image__fallback {
  display: grid;
  place-content: center;
  justify-items: center;
  gap: var(--space-2);
  padding: var(--space-8);
  color: var(--color-text-secondary);
  text-align: center;
}

.publication-image__fallback > span {
  color: var(--color-primary);
  font-size: var(--font-size-3xl);
}

.publication-image__fallback strong {
  color: var(--color-text);
}

.publication-image__fallback p {
  margin: 0;
}

.publication-image__fallback button {
  min-height: var(--control-min-size);
  padding-inline: var(--space-4);
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
  background: transparent;
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-sm);
}
</style>
