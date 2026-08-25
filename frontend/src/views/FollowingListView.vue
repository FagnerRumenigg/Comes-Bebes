<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import { useFindByUsername } from '@/api/generated/profiles/profiles'
import { useFollowing } from '@/api/generated/users/users'
import BaseAvatar from '@/components/base/BaseAvatar.vue'
import AppIcon from '@/components/icons/AppIcon.vue'
import FollowButton from '@/components/profile/FollowButton.vue'
import { useAuthStore } from '@/stores/auth.store'

const route = useRoute()
const authStore = useAuthStore()
const username = computed(() => String(route.params.username ?? ''))
const profileQuery = useFindByUsername(username)
const userId = computed(() => profileQuery.data.value?.id ?? '')

const isOwnProfile = computed(
  () =>
    Boolean(profileQuery.data.value) &&
    profileQuery.data.value?.id === authStore.identity?.userId,
)

const followingQuery = useFollowing(userId, { page: 1, size: 50 }, {
  query: { enabled: computed(() => Boolean(userId.value)) },
})
</script>

<template>
  <section class="following-list-view">
    <template v-if="profileQuery.data.value">
      <RouterLink class="following-list-view__back" :to="`/u/${username}`">
        <AppIcon name="back" :size="18" :stroke-width="2" />
        {{ isOwnProfile ? 'Meu perfil' : `Perfil de ${profileQuery.data.value.displayName}` }}
      </RouterLink>

      <h1>{{ isOwnProfile ? 'Quem eu sigo' : `Quem ${profileQuery.data.value.displayName} segue` }}</h1>

      <div v-if="followingQuery.isPending.value" class="following-list-view__state">
        Carregando...
      </div>
      <div v-else-if="!followingQuery.data.value?.content.length" class="following-list-view__empty">
        <p v-if="isOwnProfile">Você ainda não segue ninguém.</p>
        <p v-else>{{ profileQuery.data.value.displayName }} ainda não segue ninguém.</p>
      </div>
      <ul v-else class="following-list-view__list">
        <li v-for="person in followingQuery.data.value.content" :key="person.id">
          <RouterLink class="following-list-view__person" :to="`/u/${person.username}`">
            <BaseAvatar :name="person.displayName" size="small" />
            <span class="following-list-view__person-info">
              <b>{{ person.displayName }}</b>
              <span>@{{ person.username }}</span>
            </span>
          </RouterLink>
          <FollowButton
            v-if="person.id !== authStore.identity?.userId"
            :user-id="person.id"
            :following="Boolean(person.followedByCurrentUser)"
          />
        </li>
      </ul>
    </template>
  </section>
</template>

<style scoped>
.following-list-view {
  max-width: var(--content-narrow);
  margin-inline: auto;
  padding-block: var(--space-6);
}

.following-list-view__back {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  margin-block-end: var(--space-4);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  text-decoration: none;
}

.following-list-view__back:hover {
  color: var(--color-primary);
}

.following-list-view h1 {
  margin: 0 0 var(--space-6);
  font-size: var(--font-size-2xl);
}

.following-list-view__state,
.following-list-view__empty {
  padding: var(--space-8);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.following-list-view__list {
  display: grid;
  gap: var(--space-1);
  margin: 0;
  padding: 0;
  list-style: none;
}

.following-list-view__list li {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding-block: var(--space-2);
}

.following-list-view__person {
  display: flex;
  min-width: 0;
  flex: 1;
  align-items: center;
  gap: var(--space-3);
  color: inherit;
  text-decoration: none;
}

.following-list-view__person-info {
  display: grid;
  overflow: hidden;
}

.following-list-view__person-info b {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.following-list-view__person-info span {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}
</style>
