import { http } from '@/api/http'
import type { OutcomeSummaryResponse, PerformanceReportResponse, ReportFilters, ReportsOverviewResponse } from '@/types/api'

export async function getReportsOverview(filters: ReportFilters) {
  const { data } = await http.get<ReportsOverviewResponse>('/api/reports/overview', { params: filters })
  return data
}

export async function getLeadFunnel(filters: ReportFilters) {
  const { data } = await http.get<Record<string, number>>('/api/reports/funnel', { params: filters })
  return data
}

export async function getPerformanceReport(filters: ReportFilters) {
  const { data } = await http.get<PerformanceReportResponse>('/api/reports/followup-performance', { params: filters })
  return data
}

export async function getOutcomeSummary(filters: ReportFilters) {
  const { data } = await http.get<OutcomeSummaryResponse>('/api/reports/outcomes', { params: filters })
  return data
}
