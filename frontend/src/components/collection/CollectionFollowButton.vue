<script setup lang="ts">
import { ref, watch } from 'vue'
import { useMutation } from '@tanstack/vue-query'

import { followCollection, unfollowCollection } from '@/api/generated/collections/collections'
import { normalizeHttpError } from '@/api/errors'
import BaseButton from '@/components/base/BaseButton.vue'
import { showAuthNotice } from '@/composables/useAuthNotice'
import { useAuthStore } from '@/stores/auth.store'

const props = defineProps<{
  collectionId: string
  following: boolean
}>()

const emit = defineEmits<{ toggled: [] }>()

const authStore = useAuthStore()
const isFollowing = ref(props.following)
const errorMessage = ref<string | null>(null)

watch(
  () => props.following,
  (value) => {
    isFollowing.value = value
  },
)

const mutation = useMutation({
  mutationFn: (nextFollowing: boolean) =>
    nextFollowing ? followCollection(props.collectionId) : unfollowCollection(props.collectionId),
  onSuccess: () => {
    emit('toggled')
  },
  onError: (error, nextFollowing) => {
    isFollowing.value = !nextFollowing
    errorMessage.value = normalizeHttpError(error).message
  },
})

function toggle(): void {
  if (!authStore.authenticated || mutation.isPending.value) return
  errorMessage.value = null
  const nextFollowing = !isFollowing.value
  isFollowing.value = nextFollowing
  mutation.mutate(nextFollowing)
}
</script>

<template>
  <div class="collection-follow-button">
    <BaseButton v-if="!authStore.authenticated" variant="secondary" @click="showAuthNotice">
      Seguir coleção
    </BaseButton>
    <BaseButton
      v-else
      :variant="isFollowing ? 'secondary' : 'primary'"
      :loading="mutation.isPending.value"
      @click="toggle"
    >
      {{ isFollowing ? 'Deixar de seguir' : 'Seguir coleção' }}
    </BaseButton>
    <span v-if="errorMessage" class="collection-follow-button__error" role="alert">
      {{ errorMessage }}
    </span>
  </div>
</template>

<style scoped>
.collection-follow-button {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}
.collection-follow-button__error {
  color: var(--color-danger);
  font-size: var(--font-size-xs);
}
</style>
