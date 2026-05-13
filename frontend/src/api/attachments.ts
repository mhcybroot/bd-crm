import { http } from '@/api/http'
import type { AttachmentUploadResponse, DocumentLifecycleRequest, DocumentLifecycleResponse } from '@/types/api'

export async function listAttachments(leadId: number) {
  const { data } = await http.get<AttachmentUploadResponse[]>(`/api/attachments/leads/${leadId}`)
  return data
}

export async function uploadAttachment(leadId: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<AttachmentUploadResponse>(`/api/attachments/leads/${leadId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}

export async function listDocuments(leadId: number) {
  const { data } = await http.get<DocumentLifecycleResponse[]>(`/api/attachments/leads/${leadId}/documents`)
  return data
}

export async function upsertDocument(attachmentId: number, payload: DocumentLifecycleRequest) {
  const { data } = await http.put<DocumentLifecycleResponse>(`/api/attachments/${attachmentId}/document`, payload)
  return data
}
