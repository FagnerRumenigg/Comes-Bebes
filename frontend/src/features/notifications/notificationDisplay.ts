import type { IconName } from '@/components/icons/icon-paths'

import type { NotificationItem } from './notifications'

export interface NotificationTextPart {
  text: string
  bold?: boolean
}

const FALLBACK_ACTOR = 'Alguém'
const FALLBACK_PUBLICATION = 'uma publicação'
const FALLBACK_COLLECTION = 'uma coleção'

/** Texto de cada tipo de aviso (docs/telas/12-avisos.html) — sem agregação de vários atores. */
export function notificationTextParts(item: NotificationItem): NotificationTextPart[] {
  const actor = item.actorDisplayName ?? FALLBACK_ACTOR
  const title = item.publicationTitle ?? FALLBACK_PUBLICATION
  const collection = item.collectionName ?? FALLBACK_COLLECTION

  switch (item.type) {
    case 'NEW_FOLLOWER':
      return [{ text: actor, bold: true }, { text: ' começou a seguir você.' }]
    case 'FOLLOWED_USER_PUBLISHED':
      return [{ text: actor, bold: true }, { text: ' publicou algo novo.' }]
    case 'SAVED_YOUR_PUBLICATION':
      return [
        { text: actor, bold: true },
        { text: ' guardou sua publicação ' },
        { text: title, bold: true },
        { text: '.' },
      ]
    case 'REACTED_TO_YOUR_PUBLICATION':
      return [
        { text: actor, bold: true },
        { text: ' reagiu à sua publicação ' },
        { text: title, bold: true },
        { text: '.' },
      ]
    case 'MADE_YOUR_VERSION':
      return [
        { text: actor, bold: true },
        { text: ' fez a própria versão da sua ' },
        { text: title, bold: true },
        { text: '.' },
      ]
    case 'NEW_ITEM_IN_FOLLOWED_COLLECTION':
      return [{ text: collection, bold: true }, { text: ' tem coisas novas.' }]
    case 'COLLECTION_SHARED_WITH_YOU':
      return [
        { text: actor, bold: true },
        { text: ' compartilhou a coleção ' },
        { text: collection, bold: true },
        { text: ' com você.' },
      ]
    case 'REPORT_REJECTED_WARNING':
      return [{ text: 'Sua denúncia foi analisada e a publicação foi mantida.' }]
    case 'NEW_DEVICE_LOGIN':
      return [{ text: 'Novo login detectado em um dispositivo. Não foi você? Revogue o acesso.' }]
    default:
      return [{ text: 'Você recebeu um novo aviso.' }]
  }
}

const AVATAR_TYPES = new Set([
  'NEW_FOLLOWER',
  'FOLLOWED_USER_PUBLISHED',
  'SAVED_YOUR_PUBLICATION',
  'MADE_YOUR_VERSION',
  'COLLECTION_SHARED_WITH_YOU',
])

/** Quando true, mostra a inicial de quem originou o aviso em vez de um ícone. */
export function notificationShowsAvatar(item: NotificationItem): boolean {
  return AVATAR_TYPES.has(item.type) && !!item.actorDisplayName
}

const ICONS: Record<string, IconName> = {
  NEW_FOLLOWER: 'person',
  FOLLOWED_USER_PUBLISHED: 'bell',
  SAVED_YOUR_PUBLICATION: 'bookmark',
  REACTED_TO_YOUR_PUBLICATION: 'react',
  MADE_YOUR_VERSION: 'my-version',
  NEW_ITEM_IN_FOLLOWED_COLLECTION: 'collection',
  COLLECTION_SHARED_WITH_YOU: 'invite',
  REPORT_REJECTED_WARNING: 'alert',
  NEW_DEVICE_LOGIN: 'lock',
}

export function notificationIcon(item: NotificationItem): IconName {
  return ICONS[item.type] ?? 'bell'
}

const THUMB_TYPES = new Set(['SAVED_YOUR_PUBLICATION', 'REACTED_TO_YOUR_PUBLICATION', 'MADE_YOUR_VERSION'])

export function notificationShowsThumbnail(item: NotificationItem): boolean {
  return THUMB_TYPES.has(item.type) && !!item.publicationImageUrl
}

/** "Parar de avisar sobre esta coleção" — só faz sentido pra coisa nova numa coleção seguida. */
export function notificationShowsCollectionMuteOption(item: NotificationItem): boolean {
  return item.type === 'NEW_ITEM_IN_FOLLOWED_COLLECTION' && !!item.collectionId
}

/** Destino ao tocar no aviso (fora do menu de opções). */
export function notificationLinkTo(item: NotificationItem): string | null {
  if (item.type === 'NEW_DEVICE_LOGIN') return '/dispositivos'
  if (item.collectionId) return `/colecoes/${item.collectionId}`
  if (item.publicationId) return `/publicacoes/${item.publicationId}`
  return null
}
