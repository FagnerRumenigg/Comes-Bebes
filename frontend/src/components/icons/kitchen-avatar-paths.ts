export const KITCHEN_AVATAR_KEYS = [
  'PANELA',
  'COLHER_DE_PAU',
  'XICARA',
  'BOLO',
  'PAO',
  'TOMATE',
  'MILHO',
  'LIMAO',
  'TALHERES',
  'PIMENTA',
  'OVO',
  'ABACAXI',
] as const

export type KitchenAvatarKey = (typeof KITCHEN_AVATAR_KEYS)[number]

export interface KitchenAvatarDefinition {
  key: KitchenAvatarKey
  label: string
  /** Par de cor já validado na marca (marca.svg): a = fundo primary/desenho accent, b = o inverso. */
  pair: 'a' | 'b'
  /** Conteúdo interno do <svg>, sem a tile (ver KitchenAvatarIcon.vue). */
  markup: string
}

// Mesma linguagem visual da marca: formas planas, sem contorno próprio (o
// contorno vem do CSS, ver KitchenAvatarIcon.vue) — só as cores de
// tokens.css. Coordenadas dentro de um viewBox 0 0 32 32, com margem pra não
// cortar nas pontas quando a tile é um círculo (avatar é sempre redondo).
export const KITCHEN_AVATARS: KitchenAvatarDefinition[] = [
  {
    key: 'PANELA',
    label: 'Panela',
    pair: 'a',
    markup: `
      <ellipse class="ink" cx="16" cy="12.5" rx="7" ry="1.6"/>
      <circle class="ink" cx="16" cy="10.3" r="1.3"/>
      <path class="ink" d="M9.5 13.5 H22.5 L21 24.3 Q20.8 25.8 19.2 25.8 H12.8 Q11.2 25.8 11 24.3 Z"/>
      <rect class="ink" x="6.2" y="13.8" width="3.4" height="2.4" rx="1.1"/>
      <rect class="ink" x="22.4" y="13.8" width="3.4" height="2.4" rx="1.1"/>
    `,
  },
  {
    key: 'COLHER_DE_PAU',
    label: 'Colher de pau',
    pair: 'b',
    markup: `
      <ellipse class="ink" cx="16" cy="10.4" rx="4.6" ry="5.6"/>
      <rect class="ink" x="14.6" y="14.8" width="2.8" height="12.6" rx="1.4"/>
    `,
  },
  {
    key: 'XICARA',
    label: 'Xícara de café',
    pair: 'a',
    markup: `
      <path class="ink" d="M12.8 9.4c-1.7-1.1-2-2.8-.7-4.5.5 1.5 1.8 2.1.7 4.5z"/>
      <path class="ink" d="M16.6 8.3c-1.7-1.1-2-2.8-.7-4.5.5 1.5 1.8 2.1.7 4.5z"/>
      <ellipse class="ink" cx="16" cy="25.6" rx="8.2" ry="1.7"/>
      <path class="ink" d="M10.2 14.2 H21.8 L20.6 23.6 Q20.4 24.8 19.2 24.8 H12.8 Q11.6 24.8 11.4 23.6 Z"/>
      <path class="ink" d="M21.8 16.3c3 .1 4.3 1.7 4.1 3.6-.2 2-2 3.2-4.6 2.7l.3-1.8c1.5.3 2.5-.3 2.6-1.1.1-.9-.6-1.7-2.2-1.8z"/>
    `,
  },
  {
    key: 'BOLO',
    label: 'Fatia de bolo',
    pair: 'b',
    markup: `
      <path class="ink" d="M8 25 L16 8 L24 25 Z"/>
      <rect class="cut" x="10.6" y="18.8" width="10.8" height="1.6"/>
      <circle class="ink" cx="16" cy="6.8" r="1.7"/>
    `,
  },
  {
    key: 'PAO',
    label: 'Pão',
    pair: 'a',
    markup: `
      <path class="ink" d="M6.6 21.6 Q6 14 16 13.6 Q26 14 25.4 21.6 Q25.4 25.4 16 25.4 Q6.6 25.4 6.6 21.6 Z"/>
      <rect class="cut" x="10.6" y="15" width="1.6" height="6.6" rx=".8" transform="rotate(-16 11.4 18.3)"/>
      <rect class="cut" x="15.2" y="14.6" width="1.6" height="7" rx=".8"/>
      <rect class="cut" x="19.8" y="15" width="1.6" height="6.6" rx=".8" transform="rotate(16 20.6 18.3)"/>
    `,
  },
  {
    key: 'TOMATE',
    label: 'Tomate',
    pair: 'b',
    markup: `
      <circle class="ink" cx="16" cy="18.4" r="8.2"/>
      <path class="ink" d="M16 9.6 L13.6 5.8 L16 7.4 L18.4 5.8 Z"/>
    `,
  },
  {
    key: 'MILHO',
    label: 'Espiga de milho',
    pair: 'a',
    markup: `
      <ellipse class="ink" cx="16" cy="17.4" rx="5.6" ry="9.6"/>
      <path class="ink" d="M12 22.4 L8.4 25.4 L12.6 24.6Z"/>
      <path class="ink" d="M20 22.4 L23.6 25.4 L19.4 24.6Z"/>
      <circle class="cut" cx="13.3" cy="11.6" r="1"/><circle class="cut" cx="16" cy="11.6" r="1"/><circle class="cut" cx="18.7" cy="11.6" r="1"/>
      <circle class="cut" cx="13.3" cy="14.8" r="1"/><circle class="cut" cx="16" cy="14.8" r="1"/><circle class="cut" cx="18.7" cy="14.8" r="1"/>
      <circle class="cut" cx="13.3" cy="18" r="1"/><circle class="cut" cx="16" cy="18" r="1"/><circle class="cut" cx="18.7" cy="18" r="1"/>
      <circle class="cut" cx="13.3" cy="21.2" r="1"/><circle class="cut" cx="16" cy="21.2" r="1"/><circle class="cut" cx="18.7" cy="21.2" r="1"/>
    `,
  },
  {
    key: 'LIMAO',
    label: 'Limão',
    pair: 'b',
    markup: `
      <ellipse class="ink" cx="16" cy="17.2" rx="7.6" ry="8.8"/>
      <path class="ink" d="M16 7.6 L14.7 5 L17.5 5.8Z"/>
      <circle class="cut" cx="13.2" cy="13.4" r=".7"/>
      <circle class="cut" cx="18.4" cy="12.6" r=".7"/>
      <circle class="cut" cx="15.6" cy="16.2" r=".7"/>
      <circle class="cut" cx="19.4" cy="17.4" r=".7"/>
      <circle class="cut" cx="12.4" cy="18.2" r=".7"/>
      <circle class="cut" cx="16.6" cy="20.4" r=".7"/>
      <circle class="cut" cx="13.6" cy="22.4" r=".7"/>
      <circle class="cut" cx="18.2" cy="21.8" r=".7"/>
    `,
  },
  {
    key: 'TALHERES',
    label: 'Garfo e faca',
    pair: 'a',
    markup: `
      <g transform="rotate(-22 16 16)">
        <path class="ink" d="M14.9 6 H16.9 L16.3 17.4 H15.5 Z"/>
        <rect class="ink" x="15.2" y="17.4" width="1.6" height="9" rx=".8"/>
      </g>
      <g transform="rotate(22 16 16)">
        <rect class="ink" x="13.5" y="6" width="1" height="6.6" rx=".5"/>
        <rect class="ink" x="15.5" y="6" width="1" height="6.6" rx=".5"/>
        <rect class="ink" x="17.5" y="6" width="1" height="6.6" rx=".5"/>
        <path class="ink" d="M13.2 12.2 H18.8 V13.6 Q18.8 14.6 17.7 14.6 H14.3 Q13.2 14.6 13.2 13.6Z"/>
        <rect class="ink" x="15.1" y="14.4" width="1.8" height="11.6" rx=".9"/>
      </g>
    `,
  },
  {
    key: 'PIMENTA',
    label: 'Pimenta',
    pair: 'b',
    markup: `
      <path class="ink" d="M19.6 9c2.7.5 3.6 3.7 2.3 7.3-1.5 4.3-5.4 7.7-8.6 7.5-2.2-.1-3.2-1.6-2.7-3.6.4-1.6 2-2.3 1.8-4-.2-1.4-1.6-1.8-1.4-3.2.2-1.6 2.2-2.3 3.6-2 1.2.3 1.8-1.2 3.2-1.2z"/>
      <path class="ink" d="M18.8 8.4 L17.5 6.2 L20.2 6.8Z"/>
    `,
  },
  {
    key: 'OVO',
    label: 'Ovo',
    pair: 'a',
    markup: `
      <path class="ink" d="M16 6c4.4 0 7.2 6.2 7.2 11.6 0 4.8-3 7.8-7.2 7.8s-7.2-3-7.2-7.8C8.8 12.2 11.6 6 16 6z"/>
    `,
  },
  {
    key: 'ABACAXI',
    label: 'Abacaxi',
    pair: 'b',
    markup: `
      <ellipse class="ink" cx="16" cy="19.2" rx="7" ry="8.2"/>
      <path class="ink" d="M16 5.4 L17 11.4 L16 9.6 L15 11.4Z"/>
      <path class="ink" d="M12.4 6.6 L15 12 L13.6 10.6 L12.6 12.6Z"/>
      <path class="ink" d="M19.6 6.6 L17 12 L18.4 10.6 L19.4 12.6Z"/>
      <rect class="cut" x="12.6" y="14.6" width="1.4" height="1.4" transform="rotate(45 13.3 15.3)"/>
      <rect class="cut" x="16.3" y="14.6" width="1.4" height="1.4" transform="rotate(45 17 15.3)"/>
      <rect class="cut" x="14.4" y="18" width="1.4" height="1.4" transform="rotate(45 15.1 18.7)"/>
      <rect class="cut" x="18.2" y="18" width="1.4" height="1.4" transform="rotate(45 18.9 18.7)"/>
      <rect class="cut" x="12.6" y="21.4" width="1.4" height="1.4" transform="rotate(45 13.3 22.1)"/>
      <rect class="cut" x="16.3" y="21.4" width="1.4" height="1.4" transform="rotate(45 17 22.1)"/>
    `,
  },
]

export function findKitchenAvatar(key: string | null | undefined): KitchenAvatarDefinition | undefined {
  return KITCHEN_AVATARS.find((item) => item.key === key)
}
