import { http } from '@/api/http'
import type { DashboardSummaryResponse, DueFollowupResponse, ReportFilters } from '@/types/api'

export async function getDashboardSummary(filters: ReportFilters) {
  const { data } = await http.get<DashboardSummaryResponse>('/api/dashboard/summary', { params: filters })
  return data
}

export async function getWorkQueue(filters: ReportFilters) {
  const { data } = await http.get<DueFollowupResponse[]>('/api/dashboard/work-queue', { params: filters })
  return data
}
