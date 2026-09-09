/**
 * Palavras de 5 letras (pratos, ingredientes, cozinha) para o joguinho de
 * adivinhação exibido em BackendOfflineScreen.vue enquanto o backend acorda
 * de um scale-to-zero. Sem acento de propósito, em todo o jogo (lista e
 * digitação) — mais simples de jogar sem precisar de tecla morta pro til/cedilha.
 */
export const FOOD_WORDS = [
  'ARROZ',
  'BACON',
  'LIMAO',
  'MELAO',
  'MOLHO',
  'MASSA',
  'PIZZA',
  'TACOS',
  'SUSHI',
  'CALDO',
  'DOCES',
  'FRUTA',
  'CARNE',
  'PEIXE',
  'LEITE',
  'SUCOS',
  'BOLOS',
  'TORTA',
  'SALSA',
  'CANJA',
  'ALHOS',
  'COUVE',
  'ERVAS',
  'MANGA',
  'MAMAO',
  'LICOR',
  'VINHO',
  'SIDRA',
  'AIPIM',
  'MILHO',
  'GRAOS',
  'NOZES',
  'CREME',
  'PRATO',
  'FOGAO',
  'PORCO',
  'BIFES',
  'LULAS',
  'SOPAS',
  'ATUNS',
] as const

export const WORD_LENGTH = 5

export function pickRandomWord(exclude?: string): string {
  if (FOOD_WORDS.length <= 1) return FOOD_WORDS[0]
  let word: string
  do {
    word = FOOD_WORDS[Math.floor(Math.random() * FOOD_WORDS.length)]
  } while (word === exclude)
  return word
}
