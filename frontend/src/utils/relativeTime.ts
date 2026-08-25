const UNITS: Array<[Intl.RelativeTimeFormatUnit, number]> = [
  ['year', 31_536_000],
  ['month', 2_592_000],
  ['week', 604_800],
  ['day', 86_400],
  ['hour', 3_600],
  ['minute', 60],
]

const formatter = new Intl.RelativeTimeFormat('pt-BR', { numeric: 'auto' })

export function formatRelativeTime(isoDate: string): string {
  const seconds = (Date.now() - new Date(isoDate).getTime()) / 1000

  if (seconds < 60) return 'agora há pouco'

  for (const [unit, secondsInUnit] of UNITS) {
    if (seconds >= secondsInUnit) {
      return formatter.format(-Math.floor(seconds / secondsInUnit), unit)
    }
  }

  return formatter.format(-Math.floor(seconds / 60), 'minute')
}
