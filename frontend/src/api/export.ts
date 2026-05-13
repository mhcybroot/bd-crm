import { http } from '@/api/http'

export async function exportLeadsCsv() {
  const { data } = await http.get<string>('/api/exports/leads', { responseType: 'text' as const })
  return data
}
