import { http } from '@/api/http'

export async function exportFollowupsIcs(from: string, to: string): Promise<string> {
  const { data } = await http.get('/api/calendar/followups.ics', {
    params: { from, to },
    responseType: 'text',
  })
  return data
}

export async function downloadIcsFile(from: string, to: string, filename = 'followups.ics') {
  const icalContent = await exportFollowupsIcs(from, to)
  const blob = new Blob([icalContent], { type: 'text/calendar;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  URL.revokeObjectURL(url)
}