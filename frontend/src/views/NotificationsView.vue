<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import { useUnfollowCollection } from '@/api/generated/collections/collections'
import BaseAvatar from '@/components/base/BaseAvatar.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import AppIcon from '@/components/icons/AppIcon.vue'
import {
  notificationIcon,
  notificationLinkTo,
  notificationShowsAvatar,
  notificationShowsCollectionMuteOption,
  notificationShowsThumbnail,
  notificationTextParts,
} from '@/features/notifications/notificationDisplay'
import {
  formatNotificationWhen,
  groupNotificationsByDay,
  useClearNotifications,
  useDeleteNotification,
  useMarkNotificationsRead,
  useNotificationsList,
  type NotificationItem,
} from '@/features/notifications/notifications'
import { useAuthStore } from '@/stores/auth.store'
import { resolveImageUrl } from '@/utils/resolveImageUrl'

const TOAST_DURATION_MS = 8_000

const authStore = useAuthStore()
const router = useRouter()
const userId = computed(() => authStore.identity?.userId ?? '')
const enabled = computed(() => authStore.authenticated)

const notificationsQuery = useNotificationsList(userId, enabled)
const markReadMutation = useMarkNotificationsRead(userId)
const deleteMutation = useDeleteNotification(userId)
const clearMutation = useClearNotifications(userId)
const unfollowCollectionMutation = useUnfollowCollection()

// O destaque de "novo" precisa continuar visível enquanto o usuário está
// nesta tela, mesmo depois que abrir a tela marca tudo como lido no servidor
// e a lista é revalidada — por isso guarda um retrato de quem estava não
// lido na primeira carga, em vez de reler `readAt` depois de cada refetch.
const initialUnreadIds = ref<Set<string> | null>(null)
watch(
  () => notificationsQuery.data.value,
  (data) => {
    if (!data || initialUnreadIds.value !== null) return
    initialUnreadIds.value = new Set(
      data.content.filter((item) => !item.readAt).map((item) => item.id),
    )
    if (initialUnreadIds.value.size > 0) markReadMutation.mutate()
  },
  { immediate: true },
)

function isNew(item: NotificationItem): boolean {
  return initialUnreadIds.value?.has(item.id) ?? false
}

const openMenuId = ref<string | null>(null)

function closeMenu(): void {
  openMenuId.value = null
}

function toggleMenu(id: string): void {
  openMenuId.value = openMenuId.value === id ? null : id
}

function handleDocumentClick(): void {
  closeMenu()
}

function handleKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') closeMenu()
}

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
  document.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  document.removeEventListener('keydown', handleKeydown)
  flushPending()
})

// Apagar/limpar são reversíveis por alguns segundos: some da lista na hora,
// mas só chama o backend depois do prazo do toast, a menos que a pessoa
// toque em "Desfazer" antes disso (docs/telas/12-avisos.html).
const pendingDeleteId = ref<string | null>(null)
const pendingClearAll = ref(false)
const toastVisible = ref(false)
const toastMessage = ref('')
const toastUndoable = ref(true)
let toastTimer: ReturnType<typeof setTimeout> | undefined

const visibleItems = computed<NotificationItem[]>(() => {
  if (pendingClearAll.value) return []
  const content = notificationsQuery.data.value?.content ?? []
  return content.filter((item) => item.id !== pendingDeleteId.value)
})

const dayGroups = computed(() => groupNotificationsByDay(visibleItems.value))

function flushPending(): void {
  clearTimeout(toastTimer)
  if (pendingDeleteId.value) {
    deleteMutation.mutate(pendingDeleteId.value)
    pendingDeleteId.value = null
  }
  if (pendingClearAll.value) {
    clearMutation.mutate()
    pendingClearAll.value = false
  }
  toastVisible.value = false
}

function scheduleToast(message: string, undoable: boolean, onExpire: () => void): void {
  clearTimeout(toastTimer)
  toastMessage.value = message
  toastUndoable.value = undoable
  toastVisible.value = true
  toastTimer = setTimeout(() => {
    onExpire()
    toastVisible.value = false
  }, TOAST_DURATION_MS)
}

function requestDelete(item: NotificationItem): void {
  flushPending()
  closeMenu()
  pendingDeleteId.value = item.id
  scheduleToast('Aviso apagado.', true, () => {
    if (pendingDeleteId.value === item.id) {
      deleteMutation.mutate(item.id)
      pendingDeleteId.value = null
    }
  })
}

function requestClearAll(): void {
  if (!visibleItems.value.length) return
  flushPending()
  closeMenu()
  pendingClearAll.value = true
  scheduleToast('Avisos limpos.', true, () => {
    if (pendingClearAll.value) {
      clearMutation.mutate()
      pendingClearAll.value = false
    }
  })
}

function undoToast(): void {
  clearTimeout(toastTimer)
  pendingDeleteId.value = null
  pendingClearAll.value = false
  toastVisible.value = false
}

function stopCollectionNotifications(item: NotificationItem): void {
  if (!item.collectionId) return
  closeMenu()
  unfollowCollectionMutation.mutate(
    { id: item.collectionId },
    {
      onSuccess: () => {
        scheduleToast('Você não vai mais receber avisos desta coleção.', false, () => {})
      },
    },
  )
}

function linkComponentProps(item: NotificationItem): { is: typeof RouterLink | 'div'; to?: string } {
  const to = notificationLinkTo(item)
  return to ? { is: RouterLink, to } : { is: 'div' }
}
</script>

<template>
  <section class="notifications-view">
    <header class="notifications-view__header">
      <h1>Avisos</h1>
      <button
        v-if="visibleItems.length"
        type="button"
        class="notifications-view__clear"
        @click="requestClearAll"
      >
        Limpar tudo
      </button>
    </header>

    <div v-if="notificationsQuery.isPending.value" class="notifications-view__state">
      Carregando avisos...
    </div>
    <div
      v-else-if="notificationsQuery.isError.value"
      class="notifications-view__state"
      role="alert"
    >
      Não foi possível carregar seus avisos.
    </div>

    <div v-else-if="!visibleItems.length" class="notifications-view__empty">
      <AppIcon name="bell" :size="40" :stroke-width="1.4" />
      <h2>Nada por aqui ainda</h2>
      <p>Quando alguém guardar ou reagir a alguma coisa que você publicou, a gente conta.</p>
      <BaseButton @click="router.push('/publicar')">Publicar algo que você fez</BaseButton>
    </div>

    <template v-else>
      <div v-for="group in dayGroups" :key="group.label" class="notifications-view__group">
        <h2>{{ group.label }}</h2>
        <ul class="notifications-view__list">
          <li
            v-for="item in group.items"
            :key="item.id"
            class="notif-item"
            :class="{ 'notif-item--novo': isNew(item) }"
          >
            <BaseAvatar
              v-if="notificationShowsAvatar(item)"
              class="notif-item__avatar"
              :name="item.actorDisplayName ?? '?'"
            />
            <span v-else class="notif-item__icon">
              <AppIcon :name="notificationIcon(item)" :size="21" :stroke-width="1.9" />
            </span>

            <component
              :is="linkComponentProps(item).is"
              :to="linkComponentProps(item).to"
              class="notif-item__body"
            >
              <p class="notif-item__text">
                <template v-for="(part, index) in notificationTextParts(item)" :key="index">
                  <b v-if="part.bold">{{ part.text }}</b>
                  <template v-else>{{ part.text }}</template>
                </template>
              </p>
              <p class="notif-item__when">{{ formatNotificationWhen(item.createdAt) }}</p>
            </component>

            <img
              v-if="notificationShowsThumbnail(item)"
              class="notif-item__thumb"
              :src="resolveImageUrl(item.publicationImageUrl!)"
              alt=""
              loading="lazy"
            />

            <div class="notif-item__menu">
              <button
                type="button"
                class="notif-item__more"
                aria-haspopup="true"
                :aria-expanded="openMenuId === item.id"
                aria-label="Opções deste aviso"
                @click.stop="toggleMenu(item.id)"
              >
                <AppIcon name="more" :size="19" :stroke-width="2" />
              </button>
              <div v-if="openMenuId === item.id" class="notif-item__dropdown" role="menu">
                <button
                  v-if="notificationShowsCollectionMuteOption(item)"
                  type="button"
                  role="menuitem"
                  @click.stop="stopCollectionNotifications(item)"
                >
                  <AppIcon name="mute" :size="19" :stroke-width="1.9" />
                  Parar de avisar sobre esta coleção
                </button>
                <button
                  type="button"
                  role="menuitem"
                  class="notif-item__dropdown-danger"
                  @click.stop="requestDelete(item)"
                >
                  <AppIcon name="trash" :size="19" :stroke-width="2" />
                  Apagar este aviso
                </button>
              </div>
            </div>
          </li>
        </ul>
      </div>
    </template>

    <Teleport to="body">
      <div v-if="toastVisible" class="notifications-view__toast-wrapper">
        <div class="notifications-view__toast" role="status">
          <span>{{ toastMessage }}</span>
          <button v-if="toastUndoable" type="button" @click="undoToast">Desfazer</button>
        </div>
      </div>
    </Teleport>
  </section>
</template>

<style scoped>
.notifications-view {
  max-width: var(--content-narrow);
  margin-inline: auto;
}

.notifications-view__header {
  display: flex;
  align-items: baseline;
  gap: var(--space-4);
  margin-block-end: var(--space-6);
}

.notifications-view__header h1 {
  margin: 0;
  font-family: var(--font-editorial);
  font-weight: var(--font-weight-regular);
  font-size: clamp(2rem, 5vw, 2.5rem);
}

.notifications-view__clear {
  min-height: var(--control-min-size);
  padding: 0 var(--space-1);
  margin-inline-start: auto;
  color: var(--color-primary);
  font: inherit;
  font-weight: var(--font-weight-semibold);
  background: none;
  border: 0;
}

.notifications-view__clear:hover {
  text-decoration: underline;
}

.notifications-view__state {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.notifications-view__empty {
  padding: var(--space-10) var(--space-5);
  text-align: center;
}

.notifications-view__empty svg {
  margin-inline: auto;
  color: var(--color-primary);
}

.notifications-view__empty h2 {
  margin: var(--space-4) 0 var(--space-2);
  font-family: var(--font-editorial);
  font-weight: var(--font-weight-regular);
  font-size: var(--font-size-2xl);
}

.notifications-view__empty p {
  margin: 0 0 var(--space-5);
  color: var(--color-text-secondary);
}

.notifications-view__group {
  margin-block-end: var(--space-6);
}

.notifications-view__group h2 {
  margin: 0 0 var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  letter-spacing: var(--letter-spacing-wide);
  text-transform: uppercase;
}

.notifications-view__list {
  display: grid;
  gap: var(--space-2);
  padding: 0;
  list-style: none;
}

.notif-item {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-3);
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.notif-item--novo {
  background: color-mix(in srgb, var(--color-primary) 12%, var(--color-surface));
  border-color: var(--color-primary);
}

.notif-item__avatar,
.notif-item__icon {
  flex: none;
}

.notif-item__icon {
  display: grid;
  width: 2.75rem;
  height: 2.75rem;
  color: var(--color-primary);
  background: var(--color-background);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-pill);
  place-items: center;
}

.notif-item__body {
  flex: 1;
  min-width: 0;
  color: inherit;
  text-decoration: none;
}

.notif-item__text {
  margin: 0;
  font-size: var(--font-size-md);
  line-height: var(--line-height-body);
}

.notif-item__text b {
  font-weight: var(--font-weight-semibold);
}

.notif-item__when {
  margin: var(--space-1) 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.notif-item__thumb {
  flex: none;
  width: 3.25rem;
  height: 3.25rem;
  object-fit: cover;
  background: var(--color-surface-raised);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
}

.notif-item__menu {
  position: relative;
  flex: none;
}

.notif-item__more {
  display: grid;
  width: 2.75rem;
  height: 2.75rem;
  color: var(--color-text-secondary);
  background: none;
  border: 0;
  border-radius: var(--radius-md);
  place-items: center;
}

.notif-item__more:hover {
  color: var(--color-text);
  background: var(--color-background);
}

.notif-item__dropdown {
  position: absolute;
  top: calc(100% + var(--space-1));
  right: 0;
  z-index: 20;
  width: 16.5rem;
  padding: var(--space-1);
  background: var(--color-background);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
}

.notif-item__dropdown button {
  display: flex;
  width: 100%;
  min-height: var(--control-min-size);
  align-items: center;
  gap: var(--space-3);
  padding: 0 var(--space-3);
  color: var(--color-text);
  font: inherit;
  font-size: var(--font-size-sm);
  text-align: left;
  background: none;
  border: 0;
  border-radius: var(--radius-md);
}

.notif-item__dropdown button:hover {
  background: var(--color-surface);
}

.notif-item__dropdown-danger {
  color: var(--color-danger);
}

.notif-item__dropdown-danger:hover {
  background: color-mix(in srgb, var(--color-danger) 12%, var(--color-surface));
}

.notifications-view__toast-wrapper {
  position: fixed;
  z-index: 90;
  right: var(--space-4);
  bottom: calc(var(--space-4) + env(safe-area-inset-bottom));
  left: var(--space-4);
  display: flex;
  justify-content: center;
}

.notifications-view__toast {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  width: min(100%, 26rem);
  padding: var(--space-3) var(--space-4);
  color: var(--color-background);
  font-size: var(--font-size-sm);
  background: var(--color-text);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
}

.notifications-view__toast span {
  flex: 1;
}

.notifications-view__toast button {
  flex: none;
  min-height: var(--control-min-size);
  padding: 0 var(--space-1);
  color: var(--color-background);
  font: inherit;
  font-weight: var(--font-weight-bold);
  text-decoration: underline;
  background: none;
  border: 0;
}
</style>
