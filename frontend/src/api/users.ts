import { http } from '@/api/http'
import type { UserCreateRequest, UserResponse, UserRoleUpdateRequest, UserStatusUpdateRequest } from '@/types/api'

export async function listUsers() {
  const { data } = await http.get<UserResponse[]>('/api/users')
  return data
}

export async function createUser(payload: UserCreateRequest) {
  const { data } = await http.post<UserResponse>('/api/users', payload)
  return data
}

export async function updateUserRoles(id: number, payload: UserRoleUpdateRequest) {
  const { data } = await http.patch<UserResponse>(`/api/users/${id}/roles`, payload)
  return data
}

export async function updateUserStatus(id: number, payload: UserStatusUpdateRequest) {
  const { data } = await http.patch<UserResponse>(`/api/users/${id}/status`, payload)
  return data
}
