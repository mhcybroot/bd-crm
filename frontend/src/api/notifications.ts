import { http } from '@/api/http'
import type { NotificationPreferenceRequest, NotificationPreferenceResponse, NotificationResponse } from '@/types/api'

export async function listNotifications() {
  const { data } = await http.get<NotificationResponse[]>('/api/notifications')
  return data
}

export async function markNotificationRead(id: number) {
  const { data } = await http.patch<NotificationResponse>(`/api/notifications/${id}/read`)
  return data
}

export async function getNotificationPreferences() {
  const { data } = await http.get<NotificationPreferenceResponse>('/api/notifications/preferences')
  return data
}

export async function updateNotificationPreferences(payload: NotificationPreferenceRequest) {
  const { data } = await http.put<NotificationPreferenceResponse>('/api/notifications/preferences', payload)
  return data
}
