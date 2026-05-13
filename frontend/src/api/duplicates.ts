import { http } from '@/api/http'
import type { DuplicateCandidateResponse, DuplicateState, LeadMergeRequest } from '@/types/api'

export async function scanDuplicates() {
  const { data } = await http.post<DuplicateCandidateResponse[]>('/api/duplicates/scan')
  return data
}

export async function listDuplicates() {
  const { data } = await http.get<DuplicateCandidateResponse[]>('/api/duplicates')
  return data
}

export async function updateDuplicateState(id: number, state: DuplicateState) {
  const { data } = await http.patch<DuplicateCandidateResponse>(`/api/duplicates/${id}`, null, { params: { state } })
  return data
}

export async function mergeLeads(payload: LeadMergeRequest) {
  await http.post('/api/duplicates/merge', payload)
}
