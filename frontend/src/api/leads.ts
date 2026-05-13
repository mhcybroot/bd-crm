import { http } from '@/api/http'
import type {
  LeadAssignmentRequest,
  LeadDetailResponse,
  LeadNoteRequest,
  LeadNoteResponse,
  LeadRequest,
  LeadQualificationRequest,
  LeadQualificationResponse,
  LeadCommunicationRequest,
  LeadCommunicationResponse,
  LeadStageUpdateRequest,
  LeadStatus,
  LeadStatusUpdateRequest,
  LeadSummaryResponse,
  BulkLeadActionRequest,
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

export async function updateLeadStage(id: number, payload: LeadStageUpdateRequest) {
  const { data } = await http.patch<LeadDetailResponse>(`/api/leads/${id}/stage`, payload)
  return data
}

export async function addLeadNote(id: number, payload: LeadNoteRequest) {
  const { data } = await http.post<LeadNoteResponse>(`/api/leads/${id}/notes`, payload)
  return data
}

export async function updateQualification(id: number, payload: LeadQualificationRequest) {
  const { data } = await http.put<LeadQualificationResponse>(`/api/qualifications/leads/${id}`, payload)
  return data
}

export async function listCommunications(id: number) {
  const { data } = await http.get<LeadCommunicationResponse[]>(`/api/leads/${id}/communications`)
  return data
}

export async function addCommunication(id: number, payload: LeadCommunicationRequest) {
  const { data } = await http.post<LeadCommunicationResponse>(`/api/leads/${id}/communications`, payload)
  return data
}

export async function bulkLeadAction(payload: BulkLeadActionRequest) {
  const { data } = await http.post<LeadSummaryResponse[]>('/api/leads/bulk', payload)
  return data
}
