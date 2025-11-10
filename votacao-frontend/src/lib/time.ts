import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import duration from 'dayjs/plugin/duration'

dayjs.extend(utc)
dayjs.extend(duration)

/** Formata data/hora no padrão pt-BR. Aceita ISO string. */
export function formatDateTime(d?: string) {
  return d ? dayjs(d).format('DD/MM/YYYY HH:mm') : '-'
}
