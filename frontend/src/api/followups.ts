import { http } from '@/api/http'
import type { FollowupActionRequest, LeadFollowupResponse } from '@/types/api'

export async function listFollowups(status = 'open') {
  const { data } = await http.get<LeadFollowupResponse[]>('/api/followups', { params: { status } })
  return data
}

export async function completeFollowup(id: number, payload: FollowupActionRequest) {
  const { data } = await http.patch<LeadFollowupResponse>(`/api/followups/${id}/complete`, payload)
  return data
}

export async function rescheduleFollowup(id: number, payload: FollowupActionRequest) {
  const { data } = await http.patch<LeadFollowupResponse>(`/api/followups/${id}/reschedule`, payload)
  return data
}

export async function skipFollowup(id: number, payload: FollowupActionRequest) {
  const { data } = await http.patch<LeadFollowupResponse>(`/api/followups/${id}/skip`, payload)
  return data
}

export async function reassignFollowup(id: number, payload: FollowupActionRequest) {
  const { data } = await http.patch<LeadFollowupResponse>(`/api/followups/${id}/reassign`, payload)
  return data
}
