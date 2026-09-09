import { computed, ref } from 'vue'

import { pickRandomWord, WORD_LENGTH } from './words'

export const MAX_GUESSES = 6

export type LetterStatus = 'correct' | 'present' | 'absent'
export type GameStatus = 'playing' | 'won' | 'lost'

/**
 * Feedback por posição, mesma ideia clássica de jogos de adivinhação de
 * palavra: primeiro marca as letras certas na posição certa, depois marca
 * as que existem na palavra mas em outra posição — usando só o que sobrou
 * de cada letra, pra não duplicar sinal quando a tentativa repete uma letra
 * mais vezes do que ela aparece na resposta.
 */
function scoreGuess(guess: string, answer: string): LetterStatus[] {
  const result: LetterStatus[] = new Array(WORD_LENGTH).fill('absent')
  const remaining: Record<string, number> = {}

  for (let i = 0; i < WORD_LENGTH; i++) {
    if (guess[i] === answer[i]) {
      result[i] = 'correct'
    } else {
      const letter = answer[i]
      remaining[letter] = (remaining[letter] ?? 0) + 1
    }
  }

  for (let i = 0; i < WORD_LENGTH; i++) {
    if (result[i] === 'correct') continue
    const letter = guess[i]
    if (remaining[letter] > 0) {
      result[i] = 'present'
      remaining[letter] -= 1
    }
  }

  return result
}

function emptyGuess(): string[] {
  return new Array(WORD_LENGTH).fill('')
}

export function useWordGuessGame() {
  const answer = ref(pickRandomWord())
  const guesses = ref<string[]>([])
  const feedback = ref<LetterStatus[][]>([])
  // Cada posição digitável de propósito (não uma string só) - dá pra clicar
  // numa caixa do meio e continuar digitando dali, não só do começo.
  const currentGuess = ref<string[]>(emptyGuess())
  const cursor = ref(0)
  const status = ref<GameStatus>('playing')
  const message = ref('')

  const letterStatuses = computed<Record<string, LetterStatus>>(() => {
    const priority: Record<LetterStatus, number> = { absent: 0, present: 1, correct: 2 }
    const map: Record<string, LetterStatus> = {}
    guesses.value.forEach((guess, guessIndex) => {
      const rowFeedback = feedback.value[guessIndex]
      for (let i = 0; i < guess.length; i++) {
        const letter = guess[i]
        const next = rowFeedback[i]
        if (!map[letter] || priority[next] > priority[map[letter]]) {
          map[letter] = next
        }
      }
    })
    return map
  })

  function setCursor(index: number): void {
    if (status.value !== 'playing') return
    if (index < 0 || index >= WORD_LENGTH) return
    cursor.value = index
  }

  function nextEmptyFrom(start: number): number {
    for (let i = start; i < WORD_LENGTH; i++) {
      if (!currentGuess.value[i]) return i
    }
    return Math.min(start, WORD_LENGTH - 1)
  }

  function typeLetter(letter: string): void {
    if (status.value !== 'playing') return
    if (!/^[A-Z]$/.test(letter)) return
    currentGuess.value[cursor.value] = letter
    cursor.value = Math.min(cursor.value + 1, WORD_LENGTH - 1)
    // Se a próxima casa já tem letra (editando no meio), pula pra próxima vazia.
    if (currentGuess.value[cursor.value]) {
      cursor.value = nextEmptyFrom(cursor.value)
    }
  }

  function backspace(): void {
    if (status.value !== 'playing') return
    if (currentGuess.value[cursor.value]) {
      currentGuess.value[cursor.value] = ''
      return
    }
    if (cursor.value > 0) {
      cursor.value -= 1
      currentGuess.value[cursor.value] = ''
    }
  }

  function submitGuess(): void {
    if (status.value !== 'playing') return
    if (currentGuess.value.some((letter) => !letter)) {
      message.value = 'Faltam letras.'
      return
    }
    message.value = ''

    const guess = currentGuess.value.join('')
    guesses.value.push(guess)
    feedback.value.push(scoreGuess(guess, answer.value))
    currentGuess.value = emptyGuess()
    cursor.value = 0

    if (guess === answer.value) {
      status.value = 'won'
    } else if (guesses.value.length >= MAX_GUESSES) {
      status.value = 'lost'
    }
  }

  function reset(): void {
    answer.value = pickRandomWord(answer.value)
    guesses.value = []
    feedback.value = []
    currentGuess.value = emptyGuess()
    cursor.value = 0
    status.value = 'playing'
    message.value = ''
  }

  return {
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
  }
}
