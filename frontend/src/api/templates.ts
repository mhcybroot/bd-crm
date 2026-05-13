import { http } from '@/api/http'
import type {
  FollowupTemplateRequest,
  FollowupTemplateResponse,
  PipelineBoardFilters,
  PipelineBoardResponse,
  PipelineStageLeadPageResponse,
} from '@/types/api'

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

export async function getTemplateBoard(id: number, params?: PipelineBoardFilters) {
  const { data } = await http.get<PipelineBoardResponse>(`/api/pipelines/templates/${id}/board`, { params })
  return data
}

export async function getTemplateBoardStageLeads(
  templateId: number,
  stageId: number,
  params: PipelineBoardFilters & { page?: number; size?: number },
) {
  const { data } = await http.get<PipelineStageLeadPageResponse>(
    `/api/pipelines/templates/${templateId}/board/stages/${stageId}/leads`,
    { params },
  )
  return data
}
