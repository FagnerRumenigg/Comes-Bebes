<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

import { MAX_GUESSES, useWordGuessGame } from '@/features/game/useWordGuessGame'
import { WORD_LENGTH } from '@/features/game/words'

const {
  answer,
  guesses,
  feedback,
  currentGuess,
  cursor,
  status,
  message,
  letterStatuses,
  setCursor,
  typeLetter,
  backspace,
  submitGuess,
  reset,
} = useWordGuessGame()

const KEYBOARD_ROWS = ['QWERTYUIOP', 'ASDFGHJKL', 'ZXCVBNM']
const REVEAL_STEP_MS = 350
const REVEAL_HALF_MS = REVEAL_STEP_MS / 2

// Revelação letra por letra, e a cor só troca no meio do giro (quando a
// peça está de perfil, "invisível") - não no começo. Por isso são duas
// fases por letra: "hiding" (gira até de perfil, ainda sem cor) e só
// depois o feedback entra e ela termina de girar já com a cor certa.
const revealingRow = ref<number | null>(null)
const lettersShown = ref(0)
const hidingIndex = ref(-1)
// Enquanto revela, segura a UI de vitória/derrota (senão "Acertou!" aparece
// antes do flip terminar).
const revealing = computed(() => revealingRow.value !== null)

const rows = computed(() => {
  const filledRows = guesses.value.map((guess, index) => ({
    letters: guess.split(''),
    feedback:
      index === revealingRow.value ? feedback.value[index].slice(0, lettersShown.value) : feedback.value[index],
    active: false,
  }))
  const activeRow =
    status.value === 'playing' && guesses.value.length < MAX_GUESSES
      ? [{ letters: [...currentGuess.value], feedback: null, active: true }]
      : []
  const emptyCount = MAX_GUESSES - filledRows.length - activeRow.length
  const emptyRows = Array.from({ length: Math.max(emptyCount, 0) }, () => ({
    letters: new Array(WORD_LENGTH).fill(''),
    feedback: null,
    active: false,
  }))
  return [...filledRows, ...activeRow, ...emptyRows]
})

const shake = ref(false)
const activeRowIndex = computed(() => (status.value === 'playing' ? guesses.value.length : -1))

function handleSubmit(): void {
  const guessesBefore = guesses.value.length
  submitGuess()

  if (message.value) {
    shake.value = true
    setTimeout(() => (shake.value = false), 400)
    return
  }
  if (guesses.value.length === guessesBefore) return

  const rowIndex = guesses.value.length - 1
  revealingRow.value = rowIndex
  lettersShown.value = 0
  hidingIndex.value = -1

  for (let i = 0; i < WORD_LENGTH; i++) {
    setTimeout(() => {
      hidingIndex.value = i
    }, i * REVEAL_STEP_MS)
    setTimeout(() => {
      hidingIndex.value = -1
      lettersShown.value = i + 1
    }, i * REVEAL_STEP_MS + REVEAL_HALF_MS)
  }
  // Espera a última letra terminar de levantar (não só trocar de cor) antes
  // de liberar a UI de vitória/derrota.
  setTimeout(
    () => {
      revealingRow.value = null
    },
    WORD_LENGTH * REVEAL_STEP_MS,
  )
}

function handleKeydown(event: KeyboardEvent): void {
  if (event.metaKey || event.ctrlKey || event.altKey) return
  if (event.key === 'Enter') {
    handleSubmit()
    return
  }
  if (event.key === 'Backspace') {
    backspace()
    return
  }
  const letter = event.key.toUpperCase()
  if (/^[A-Z]$/.test(letter)) typeLetter(letter)
}

onMounted(() => document.addEventListener('keydown', handleKeydown))
onBeforeUnmount(() => document.removeEventListener('keydown', handleKeydown))
</script>

<template>
  <div class="word-guess-game">
    <p class="word-guess-game__hint">Adivinhe o prato ou ingrediente — 5 letras, 6 tentativas.</p>

    <div class="word-guess-game__board" role="group" aria-label="Tabuleiro do jogo">
      <div
        v-for="(row, rowIndex) in rows"
        :key="rowIndex"
        class="word-guess-game__row"
        :class="{ 'word-guess-game__row--shake': shake && rowIndex === activeRowIndex }"
      >
        <span
          v-for="(letter, letterIndex) in row.letters"
          :key="letterIndex"
          class="word-guess-game__tile"
          :class="[
            row.feedback ? `word-guess-game__tile--${row.feedback[letterIndex]}` : undefined,
            rowIndex === revealingRow && letterIndex === hidingIndex
              ? 'word-guess-game__tile--hiding'
              : undefined,
            row.active && letterIndex === cursor ? 'word-guess-game__tile--cursor' : undefined,
            row.active ? 'word-guess-game__tile--clickable' : undefined,
          ]"
          @click="row.active && setCursor(letterIndex)"
        >
          {{ letter }}
        </span>
      </div>
    </div>

    <p v-if="message" class="word-guess-game__message" role="alert">{{ message }}</p>
    <p
      v-else-if="status === 'won' && !revealing"
      class="word-guess-game__message word-guess-game__message--won"
    >
      Acertou!
    </p>
    <p v-else-if="status === 'lost' && !revealing" class="word-guess-game__message">
      Não dessa vez. Era "{{ answer }}".
    </p>

    <button
      v-if="status !== 'playing' && !revealing"
      type="button"
      class="word-guess-game__again"
      @click="reset"
    >
      Jogar de novo
    </button>

    <div v-else class="word-guess-game__keyboard" role="group" aria-label="Teclado do jogo">
      <div v-for="(keyRow, index) in KEYBOARD_ROWS" :key="index" class="word-guess-game__key-row">
        <button
          v-for="letter in keyRow"
          :key="letter"
          type="button"
          class="word-guess-game__key"
          :class="letterStatuses[letter] ? `word-guess-game__key--${letterStatuses[letter]}` : undefined"
          @click="typeLetter(letter)"
        >
          {{ letter }}
        </button>
      </div>
      <div class="word-guess-game__key-row">
        <button type="button" class="word-guess-game__key word-guess-game__key--wide" @click="backspace">
          Apagar
        </button>
        <button type="button" class="word-guess-game__key word-guess-game__key--wide" @click="handleSubmit">
          Enviar
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.word-guess-game {
  display: grid;
  gap: var(--space-3);
  justify-items: center;
  width: 100%;
  max-width: 22rem;
  margin-inline: auto;
}

.word-guess-game__hint {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  text-align: center;
}

.word-guess-game__board {
  display: grid;
  gap: 0.375rem;
}

.word-guess-game__row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 0.375rem;
}

.word-guess-game__tile {
  display: grid;
  width: 2.75rem;
  height: 2.75rem;
  align-items: center;
  justify-items: center;
  color: var(--color-text);
  font-weight: var(--font-weight-bold);
  font-size: var(--font-size-lg);
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  /* Base sempre com a transição, pra funcionar nos dois sentidos do giro
     (deitando e voltando) não importa qual classe mudou o transform. */
  transition: transform 0.175s linear;
}

.word-guess-game__tile--clickable {
  cursor: pointer;
}

.word-guess-game__tile--cursor {
  border-color: var(--color-primary);
  border-width: 2px;
}

/* Revela uma letra de cada vez, esquerda pra direita: primeiro deita de
   perfil ainda sem cor (--hiding, ver template/script), só troca de cor
   exatamente nesse instante "invisível" e então levanta de novo já
   revelada — em vez de mostrar a cor certa antes do giro acabar. */
.word-guess-game__tile--hiding {
  transform: rotateX(90deg);
}

.word-guess-game__tile--correct {
  color: var(--color-primary-contrast);
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.word-guess-game__tile--present {
  color: var(--color-text);
  background: var(--color-accent);
  border-color: var(--color-accent);
}

.word-guess-game__tile--absent {
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border-color: var(--color-border);
}

.word-guess-game__message {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  text-align: center;
}

.word-guess-game__message--won {
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
}

.word-guess-game__again {
  min-height: var(--control-min-size);
  padding-inline: var(--space-5);
  color: var(--color-primary-contrast);
  font: inherit;
  font-weight: var(--font-weight-semibold);
  background: var(--color-primary);
  border: 0;
  border-radius: var(--radius-sm);
}

.word-guess-game__keyboard {
  display: grid;
  gap: 0.375rem;
  width: 100%;
}

.word-guess-game__key-row {
  display: flex;
  justify-content: center;
  gap: 0.25rem;
}

.word-guess-game__key {
  min-width: 1.75rem;
  min-height: 2.5rem;
  padding-inline: var(--space-2);
  color: var(--color-text);
  font: inherit;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.word-guess-game__key--wide {
  min-width: 4rem;
  font-size: var(--font-size-xs);
}

.word-guess-game__key--correct {
  color: var(--color-primary-contrast);
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.word-guess-game__key--present {
  color: var(--color-text);
  background: var(--color-accent);
  border-color: var(--color-accent);
}

.word-guess-game__key--absent {
  /* Preto fixo (não um token de tema) de propósito: sinaliza "essa letra já
     não serve mais" de um jeito que não muda com claro/escuro, igual a
     convenção de outros jogos do gênero. */
  color: #ffffff;
  background: #1a1a1a;
  border-color: #1a1a1a;
}

.word-guess-game__row--shake {
  animation: word-guess-game-shake var(--duration-moderate) var(--ease-standard);
}

@keyframes word-guess-game-shake {
  0%,
  100% {
    transform: translateX(0);
  }

  25% {
    transform: translateX(-0.375rem);
  }

  75% {
    transform: translateX(0.375rem);
  }
}

@media (prefers-reduced-motion: reduce) {
  .word-guess-game__tile {
    transition: none;
  }

  .word-guess-game__row--shake {
    animation: none;
  }
}
</style>
