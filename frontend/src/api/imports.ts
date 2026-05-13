import { http } from '@/api/http'
import type { LeadImportPreviewRequest, LeadImportPreviewResponse, LeadImportResultResponse } from '@/types/api'

function buildImportFormData(file: File, payload: LeadImportPreviewRequest) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('importMode', payload.importMode)
  formData.append('columnMappings', JSON.stringify(payload.columnMappings))
  return formData
}

export async function downloadLeadImportTemplate() {
  const { data } = await http.get<Blob>('/api/imports/leads/template', {
    responseType: 'blob',
  })
  return data
}

export async function previewLeadImport(file: File, payload: LeadImportPreviewRequest) {
  const { data } = await http.post<LeadImportPreviewResponse>(
    '/api/imports/leads/preview',
    buildImportFormData(file, payload),
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  )
  return data
}

export async function importLeads(file: File, payload: LeadImportPreviewRequest) {
  const { data } = await http.post<LeadImportResultResponse>(
    '/api/imports/leads',
    buildImportFormData(file, payload),
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  )
  return data
}
