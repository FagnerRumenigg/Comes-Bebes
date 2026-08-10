<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'

import { apiRequest, requiresNgrokBrowserWarningBypass } from '@/api/client'

const props = defineProps<{
  src: string
  alt: string
}>()

const failed = ref(false)
const reloadKey = ref(0)
const fetchedSrc = ref<string | null>(null)
const backendBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8082'
let assetRequestController: AbortController | null = null

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
const requiresNgrokAssetRequest = computed(() =>
  requiresNgrokBrowserWarningBypass(resolvedSrc.value),
)

const displayedSrc = computed(() =>
  requiresNgrokAssetRequest.value ? fetchedSrc.value : resolvedSrc.value,
)

function clearFetchedAsset(): void {
  assetRequestController?.abort()
  assetRequestController = null
  if (fetchedSrc.value) URL.revokeObjectURL(fetchedSrc.value)
  fetchedSrc.value = null
}

async function loadImage(): Promise<void> {
  failed.value = false
  reloadKey.value += 1

  if (!requiresNgrokAssetRequest.value) return

  clearFetchedAsset()
  const controller = new AbortController()
  assetRequestController = controller

  try {
    const image = await apiRequest<Blob>({
      url: resolvedSrc.value,
      method: 'GET',
      responseType: 'blob',
      headers: { Accept: 'image/*' },
      signal: controller.signal,
    })
    if (controller.signal.aborted) return
    fetchedSrc.value = URL.createObjectURL(image)
  } catch {
    if (!controller.signal.aborted) failed.value = true
  }
}

watch(
  () => props.src,
  () => {
    void loadImage()
  },
  { immediate: true },
)

function retry(): void {
  void loadImage()
}

onBeforeUnmount(clearFetchedAsset)
</script>

<template>
  <div class="publication-image">
    <img
      v-if="!failed && displayedSrc"
      :key="reloadKey"
      :src="displayedSrc"
      :alt="alt"
      loading="eager"
      @error="failed = true"
    />
    <div v-else-if="failed" class="publication-image__fallback" role="status">
      <span aria-hidden="true">◇</span>
      <strong>A imagem não está mais disponível.</strong>
      <p>A URL pode ter expirado. Tente carregá-la novamente.</p>
      <button type="button" @click="retry">Tentar novamente</button>
    </div>
    <div v-else class="publication-image__loading" role="status" aria-label="Carregando imagem" />
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

.publication-image__loading {
  background: linear-gradient(
    100deg,
    color-mix(in srgb, var(--color-border) 32%, var(--color-surface)) 30%,
    color-mix(in srgb, var(--color-border) 58%, var(--color-surface)) 50%,
    color-mix(in srgb, var(--color-border) 32%, var(--color-surface)) 70%
  );
  background-size: 200% 100%;
  animation: publication-image-loading var(--duration-slow) linear infinite;
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

@keyframes publication-image-loading {
  to {
    background-position: -200% 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .publication-image__loading {
    animation: none;
  }
}
</style>
