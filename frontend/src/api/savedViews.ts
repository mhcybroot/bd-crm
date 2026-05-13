import { http } from '@/api/http'
import type { SavedViewRequest, SavedViewResponse } from '@/types/api'

export async function listSavedViews(pageKey: string) {
  const { data } = await http.get<SavedViewResponse[]>('/api/saved-views', { params: { pageKey } })
  return data
}

export async function saveView(payload: SavedViewRequest) {
  const { data } = await http.post<SavedViewResponse>('/api/saved-views', payload)
  return data
}
