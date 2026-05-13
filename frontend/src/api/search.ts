import { http } from '@/api/http'
import type { GlobalSearchResponse } from '@/types/api'

export async function globalSearch(params: Record<string, unknown>) {
  const { data } = await http.get<GlobalSearchResponse>('/api/search', { params })
  return data
}
