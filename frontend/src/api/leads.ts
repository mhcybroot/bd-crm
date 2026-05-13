import { http } from '@/api/http'
import type {
  LeadAssignmentRequest,
  LeadDetailResponse,
  LeadNoteRequest,
  LeadNoteResponse,
  LeadRequest,
  LeadStatus,
  LeadStatusUpdateRequest,
  LeadSummaryResponse,
  PagedResponse,
} from '@/types/api'

export interface LeadListParams {
  page?: number
  size?: number
  status?: LeadStatus | ''
  assignedUserId?: number | null
  search?: string
}

export async function listLeads(params: LeadListParams) {
  const { data } = await http.get<PagedResponse<LeadSummaryResponse>>('/api/leads', { params })
  return data
}

export async function getLead(id: number) {
  const { data } = await http.get<LeadDetailResponse>(`/api/leads/${id}`)
  return data
}

export async function createLead(payload: LeadRequest) {
  const { data } = await http.post<LeadDetailResponse>('/api/leads', payload)
  return data
}

export async function updateLead(id: number, payload: LeadRequest) {
  const { data } = await http.put<LeadDetailResponse>(`/api/leads/${id}`, payload)
  return data
}

export async function assignLead(id: number, payload: LeadAssignmentRequest) {
  const { data } = await http.patch<LeadDetailResponse>(`/api/leads/${id}/assign`, payload)
  return data
}

export async function updateLeadStatus(id: number, payload: LeadStatusUpdateRequest) {
  const { data } = await http.patch<LeadDetailResponse>(`/api/leads/${id}/status`, payload)
  return data
}

export async function addLeadNote(id: number, payload: LeadNoteRequest) {
  const { data } = await http.post<LeadNoteResponse>(`/api/leads/${id}/notes`, payload)
  return data
}
