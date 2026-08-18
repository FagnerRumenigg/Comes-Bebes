import { reactive } from 'vue'
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import AdminLayout from '@/app/layouts/AdminLayout.vue'
import AppLayout from '@/app/layouts/AppLayout.vue'
import AuthLayout from '@/app/layouts/AuthLayout.vue'
import FeedView from '@/views/FeedView.vue'
import PublicationDetailsView from '@/views/PublicationDetailsView.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import CreatePublicationView from '@/views/CreatePublicationView.vue'
import DraftsView from '@/views/DraftsView.vue'
import EditPublicationView from '@/views/EditPublicationView.vue'
import ProfileView from '@/views/ProfileView.vue'
import SearchView from '@/views/SearchView.vue'
import SavedView from '@/views/SavedView.vue'
import OnboardingView from '@/views/OnboardingView.vue'
import NotificationsView from '@/views/NotificationsView.vue'
import DevicesView from '@/views/DevicesView.vue'
import ModerationQueueView from '@/views/ModerationQueueView.vue'
import ModerationCaseView from '@/views/ModerationCaseView.vue'
import NotFoundView from '@/views/NotFoundView.vue'

import { routeAccessGuard } from './guards'

export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: AppLayout,
    children: [
      {
        path: '',
        name: 'feed',
        component: FeedView,
        meta: { access: 'public', pageTitle: 'Feed' },
      },
      {
        path: 'buscar',
        name: 'search',
        component: SearchView,
        meta: { access: 'public', pageTitle: 'Buscar' },
      },
      {
        path: 'publicacoes/:id/editar',
        name: 'edit-publication',
        component: EditPublicationView,
        meta: { access: 'authenticated', pageTitle: 'Editar publicação' },
      },
      {
        path: 'publicacoes/:id',
        name: 'publication-details',
        component: PublicationDetailsView,
        meta: { access: 'public', pageTitle: 'Detalhes da publicação' },
      },
      {
        path: 'u/:username',
        name: 'profile',
        component: ProfileView,
        meta: { access: 'public', pageTitle: 'Perfil' },
      },
      {
        path: 'publicar',
        name: 'create-publication',
        component: CreatePublicationView,
        meta: { access: 'authenticated', pageTitle: 'Criar publicação' },
      },
      {
        path: 'publicar/minha-versao/:sourceId',
        name: 'create-my-version',
        component: CreatePublicationView,
        meta: { access: 'authenticated', pageTitle: 'Publicar minha versão' },
      },
      {
        path: 'publicar/rascunho/:draftId',
        name: 'resume-draft',
        component: CreatePublicationView,
        meta: { access: 'authenticated', pageTitle: 'Continuar rascunho' },
      },
      {
        path: 'rascunhos',
        name: 'drafts',
        component: DraftsView,
        meta: { access: 'authenticated', pageTitle: 'Rascunhos' },
      },
      {
        path: 'salvos',
        name: 'saved',
        component: SavedView,
        meta: { access: 'authenticated', pageTitle: 'Publicações salvas' },
      },
      {
        path: 'notificacoes',
        name: 'notifications',
        component: NotificationsView,
        meta: { access: 'authenticated', pageTitle: 'Notificações' },
      },
      {
        path: 'dispositivos',
        name: 'devices',
        component: DevicesView,
        meta: { access: 'authenticated', pageTitle: 'Meus dispositivos' },
      },
      {
        path: 'onboarding',
        name: 'onboarding',
        component: OnboardingView,
        meta: { access: 'authenticated', pageTitle: 'Bem-vindo' },
      },
      {
        path: ':pathMatch(.*)*',
        name: 'not-found',
        component: NotFoundView,
        meta: { access: 'public', pageTitle: 'Página não encontrada' },
      },
    ],
  },
  {
    path: '/',
    component: AuthLayout,
    children: [
      {
        path: 'cadastro',
        name: 'register',
        component: RegisterView,
        meta: { access: 'guest', pageTitle: 'Criar cadastro' },
      },
      {
        path: 'login',
        name: 'login',
        component: LoginView,
        meta: { access: 'guest', pageTitle: 'Entrar' },
      },
    ],
  },
  {
    path: '/admin',
    component: AdminLayout,
    children: [
      {
        path: 'moderacao',
        name: 'moderation-queue',
        component: ModerationQueueView,
        meta: { access: 'admin', pageTitle: 'Fila de moderação' },
      },
      {
        path: 'moderacao/:caseId',
        name: 'moderation-case',
        component: ModerationCaseView,
        meta: { access: 'admin', pageTitle: 'Análise de moderação' },
      },
    ],
  },
]

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export const navigationState = reactive({
  pending: false,
  error: null as string | null,
})

export function dismissNavigationError(): void {
  navigationState.error = null
}

router.beforeEach(async (to) => {
  navigationState.pending = true
  navigationState.error = null
  return routeAccessGuard(to)
})

router.afterEach((to) => {
  navigationState.pending = false
  document.title = `${String(to.meta.pageTitle ?? 'Comes&Bebes')} | Comes&Bebes`
})

router.onError(() => {
  navigationState.pending = false
  navigationState.error = 'Não foi possível abrir esta página. Tente novamente.'
})
