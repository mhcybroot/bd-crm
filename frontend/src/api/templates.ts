import { http } from '@/api/http'
import type { FollowupTemplateRequest, FollowupTemplateResponse, PipelineBoardResponse } from '@/types/api'

export async function listTemplates() {
  const { data } = await http.get<FollowupTemplateResponse[]>('/api/followup-templates')
  return data
}

export async function getTemplate(id: number) {
  const { data } = await http.get<FollowupTemplateResponse>(`/api/followup-templates/${id}`)
  return data
}

export async function createTemplate(payload: FollowupTemplateRequest) {
  const { data } = await http.post<FollowupTemplateResponse>('/api/followup-templates', payload)
  return data
}

export async function updateTemplate(id: number, payload: FollowupTemplateRequest) {
  const { data } = await http.put<FollowupTemplateResponse>(`/api/followup-templates/${id}`, payload)
  return data
}

export async function getTemplateBoard(id: number) {
  const { data } = await http.get<PipelineBoardResponse>(`/api/pipelines/templates/${id}/board`)
  return data
}
