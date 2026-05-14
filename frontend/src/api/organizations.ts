import { http } from '@/api/http'
import type {
  OrganizationBootstrapRequest,
  OrganizationBootstrapResponse,
  OrganizationRequest,
  OrganizationResponse,
} from '@/types/api'

export async function listOrganizations() {
  const { data } = await http.get<OrganizationResponse[]>('/api/organizations')
  return data
}

export async function bootstrapOrganization(payload: OrganizationBootstrapRequest) {
  const { data } = await http.post<OrganizationBootstrapResponse>('/api/organizations/bootstrap', payload)
  return data
}

export async function updateOrganization(id: number, payload: OrganizationRequest) {
  const { data } = await http.put<OrganizationResponse>(`/api/organizations/${id}`, payload)
  return data
}
