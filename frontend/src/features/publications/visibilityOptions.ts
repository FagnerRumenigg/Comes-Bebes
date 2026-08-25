// Vocabulário exato de produto5.md v5 §4/§6.4 — "Interno" está banido da
// interface. Usado tanto em Publicar (docs/telas/10) quanto em Configurações
// → Minha conta, "Quem pode ver o que você publica" (docs/telas/09).
export const PUBLICATION_VISIBILITY_OPTIONS = [
  {
    value: 'PUBLIC',
    title: 'Público',
    description: 'Qualquer pessoa vê, mesmo quem não tem conta no Comes&Bebes.',
  },
  {
    value: 'INTERNAL',
    title: 'Só para quem tem conta',
    description: 'Só é vista por quem entrou no Comes&Bebes.',
  },
  {
    value: 'PRIVATE',
    title: 'Só para mim',
    description: 'Fica como um caderno seu. Ninguém além de você vê.',
  },
]
