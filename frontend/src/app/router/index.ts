import { reactive } from 'vue'
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import AdminLayout from '@/app/layouts/AdminLayout.vue'
import AppLayout from '@/app/layouts/AppLayout.vue'
import AuthLayout from '@/app/layouts/AuthLayout.vue'
import FeedView from '@/views/FeedView.vue'
import WelcomeView from '@/views/WelcomeView.vue'
import PublicationDetailsView from '@/views/PublicationDetailsView.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import CreatePublicationView from '@/views/CreatePublicationView.vue'
import DraftsView from '@/views/DraftsView.vue'
import EditPublicationView from '@/views/EditPublicationView.vue'
import ProfileView from '@/views/ProfileView.vue'
import EditProfileView from '@/views/EditProfileView.vue'
import FollowingListView from '@/views/FollowingListView.vue'
import ForgotPasswordView from '@/views/ForgotPasswordView.vue'
import SearchView from '@/views/SearchView.vue'
import SavedView from '@/views/SavedView.vue'
import AcceptCollectionInviteView from '@/views/AcceptCollectionInviteView.vue'
import CollectionDetailsView from '@/views/CollectionDetailsView.vue'
import RequireEmailView from '@/views/RequireEmailView.vue'
import NotificationsView from '@/views/NotificationsView.vue'
import DevicesView from '@/views/DevicesView.vue'
import SettingsView from '@/views/SettingsView.vue'
import DocumentView from '@/views/DocumentView.vue'
import FeedbackView from '@/views/FeedbackView.vue'
import ModerationQueueView from '@/views/ModerationQueueView.vue'
import ModerationCaseView from '@/views/ModerationCaseView.vue'
import NotFoundView from '@/views/NotFoundView.vue'

import { routeAccessGuard } from './guards'

export const routes: RouteRecordRaw[] = [
  {
    path: '/bem-vindo',
    name: 'welcome',
    component: WelcomeView,
    meta: { access: 'public', pageTitle: 'Bem-vindo' },
  },
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
        path: 'perfil/editar',
        name: 'edit-profile',
        component: EditProfileView,
        meta: { access: 'authenticated', pageTitle: 'Editar perfil' },
      },
      {
        path: 'u/:username',
        name: 'profile',
        component: ProfileView,
        meta: { access: 'public', pageTitle: 'Perfil' },
      },
      {
        path: 'u/:username/seguindo',
        name: 'following',
        component: FollowingListView,
        meta: { access: 'public', pageTitle: 'Quem segue' },
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
        path: 'colecoes/convite/:token',
        name: 'accept-collection-invite',
        component: AcceptCollectionInviteView,
        meta: { access: 'authenticated', pageTitle: 'Aceitar convite' },
      },
      {
        path: 'colecoes/:id',
        name: 'collection-details',
        component: CollectionDetailsView,
        meta: { access: 'public', pageTitle: 'Coleção' },
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
        path: 'configuracoes/:secao?',
        name: 'settings',
        component: SettingsView,
        meta: { access: 'authenticated', pageTitle: 'Configurações' },
      },
      {
        path: 'informar-email',
        name: 'require-email',
        component: RequireEmailView,
        meta: { access: 'authenticated', pageTitle: 'Informar e-mail' },
      },
      {
        path: 'termos',
        name: 'terms',
        component: DocumentView,
        meta: { access: 'public', pageTitle: 'Termos de Serviço', documentSlug: 'TERMS_OF_SERVICE' },
      },
      {
        path: 'privacidade',
        name: 'privacy',
        component: DocumentView,
        meta: { access: 'public', pageTitle: 'Política de Privacidade', documentSlug: 'PRIVACY_POLICY' },
      },
      {
        path: 'faq',
        name: 'faq',
        component: DocumentView,
        meta: { access: 'public', pageTitle: 'Como usar o Comes&Bebes', documentSlug: 'FAQ' },
      },
      {
        path: 'sugestao',
        name: 'feedback',
        component: FeedbackView,
        meta: { access: 'authenticated', pageTitle: 'Falar com a gente' },
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
      {
        path: 'recuperar-senha/:token?',
        name: 'forgot-password',
        component: ForgotPasswordView,
        meta: { access: 'guest', pageTitle: 'Recuperar senha' },
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
