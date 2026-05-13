import { http } from '@/api/http'
import type { LeadImportPreviewResponse, LeadImportResultResponse } from '@/types/api'

export async function previewLeadImport(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<LeadImportPreviewResponse>('/api/imports/leads/preview', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}

export async function importLeads(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<LeadImportResultResponse>('/api/imports/leads', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}
