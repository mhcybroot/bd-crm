import { http } from '@/api/http'
import type { AuthResponse, AuthSessionResponse, LoginRequest } from '@/types/api'

export async function login(payload: LoginRequest) {
  const { data } = await http.post<AuthSessionResponse>('/api/auth/login', payload)
  return data
}

export async function getCurrentUser() {
  const { data } = await http.get<AuthResponse>('/api/auth/me')
  return data
}
