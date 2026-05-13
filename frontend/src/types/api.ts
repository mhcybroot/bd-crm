export type RoleName = 'ADMIN' | 'MANAGER' | 'REP'
export type LeadStatus = 'NEW' | 'IN_PROGRESS' | 'QUALIFIED' | 'WON' | 'LOST' | 'DORMANT'
export type LeadPriority = 'LOW' | 'MEDIUM' | 'HIGH'
export type FollowupStatus = 'DUE' | 'OVERDUE' | 'COMPLETED' | 'SKIPPED' | 'CANCELLED'
export type ContactChannel = 'CALL' | 'EMAIL' | 'WHATSAPP' | 'LINKEDIN' | 'MEETING'
export type FollowupOutcome =
  | 'NO_RESPONSE'
  | 'INTERESTED'
  | 'NOT_INTERESTED'
  | 'CALLBACK_REQUESTED'
  | 'QUALIFIED'
  | 'WON'
  | 'LOST'

export interface ApiErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  details: string[]
}

export interface AuthResponse {
  id: number
  username: string
  fullName: string
  email: string
  roles: RoleName[]
}

export interface AuthSessionResponse {
  token: string
  user: AuthResponse
}

export interface PagedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export interface LeadSummaryResponse {
  id: number
  companyName: string
  contactName: string
  email: string | null
  phone: string | null
  source: string | null
  status: LeadStatus
  priority: LeadPriority
  assignedUserId: number
  assignedUserName: string
  templateId: number
  templateName: string
}

export interface LeadNoteResponse {
  id: number
  body: string
  authorId: number
  authorName: string
  createdAt: string
}

export interface LeadActivityResponse {
  id: number
  type: string
  description: string
  actorId: number
  actorName: string
  createdAt: string
}

export interface LeadFollowupResponse {
  id: number
  stepNumber: number
  dueDate: string
  assignedUserId: number
  assignedUserName: string
  status: FollowupStatus
  channel: ContactChannel
  outcome: FollowupOutcome | null
  instructions: string | null
  notes: string | null
  completedAt: string | null
  escalatedAt: string | null
}

export interface LeadDetailResponse {
  lead: LeadSummaryResponse
  followups: LeadFollowupResponse[]
  notes: LeadNoteResponse[]
  activities: LeadActivityResponse[]
}

export interface DashboardSummaryResponse {
  leadsTotal: number
  newLeadsInRange: number
  leadsByStatus: Record<string, number>
  dueToday: number
  overdue: number
  escalated: number
  completedFollowups: number
  completionRate: number
  topOutcomes: Record<string, number>
}

export interface DueFollowupResponse {
  followupId: number
  leadId: number
  companyName: string
  contactName: string
  stepNumber: number
  dueDate: string
  status: FollowupStatus
  channel: ContactChannel
  assignedUserId: number
  assignedUserName: string
  escalated: boolean
}

export interface TemplateStepResponse {
  id: number
  stepNumber: number
  dayOffset: number
  channel: ContactChannel
  instructions: string | null
}

export interface FollowupTemplateResponse {
  id: number
  name: string
  description: string | null
  isDefault: boolean
  active: boolean
  steps: TemplateStepResponse[]
}

export interface UserResponse {
  id: number
  username: string
  fullName: string
  email: string
  active: boolean
  managerId: number | null
  roles: RoleName[]
}

export interface PerformanceRepResponse {
  userId: number
  userName: string
  assignedLeads: number
  pendingFollowups: number
  completedFollowups: number
}

export interface PerformanceReportResponse {
  leadFunnel: Record<string, number>
  completionRate: number
  outcomes: OutcomeSummaryResponse
  trends: TrendPointResponse[]
  reps: PerformanceRepResponse[]
}

export interface OutcomeSummaryResponse {
  outcomes: Record<string, number>
  unknownOutcomeCount: number
}

export interface TrendPointResponse {
  date: string
  leadsCreated: number
  followupsCompleted: number
  wonCount: number
  lostCount: number
}

export interface ReportKpiSummaryResponse {
  leadsInRange: number
  followupsInRange: number
  followupsCompleted: number
  overdueFollowups: number
  completionRate: number
  wonLeads: number
  lostLeads: number
}

export interface ReportsOverviewResponse {
  summary: ReportKpiSummaryResponse
  funnel: Record<string, number>
  outcomes: OutcomeSummaryResponse
  trends: TrendPointResponse[]
  reps: PerformanceRepResponse[]
}

export interface ReportFilters {
  dateFrom?: string
  dateTo?: string
  repUserId?: number | null
  leadStatus?: LeadStatus | ''
  followupOutcome?: FollowupOutcome | ''
  source?: string
  templateId?: number | null
  channel?: ContactChannel | ''
  priority?: LeadPriority | ''
  escalated?: boolean | null
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LeadRequest {
  companyName: string
  contactName: string
  email: string | null
  phone: string | null
  source: string | null
  description: string | null
  priority: LeadPriority
  assignedUserId: number | null
  templateId: number | null
}

export interface LeadAssignmentRequest {
  assignedUserId: number
}

export interface LeadStatusUpdateRequest {
  status: LeadStatus
}

export interface LeadNoteRequest {
  body: string
}

export interface FollowupActionRequest {
  dueDate?: string | null
  outcome?: FollowupOutcome | null
  assignedUserId?: number | null
  notes?: string | null
}

export interface TemplateStepRequest {
  stepNumber: number
  dayOffset: number
  channel: ContactChannel
  instructions: string
}

export interface FollowupTemplateRequest {
  name: string
  description: string
  isDefault: boolean
  active: boolean
  steps: TemplateStepRequest[]
}

export interface UserCreateRequest {
  username: string
  password: string
  fullName: string
  email: string
  managerId: number | null
  roles: RoleName[]
}

export interface UserRoleUpdateRequest {
  roles: RoleName[]
}

export interface UserStatusUpdateRequest {
  active: boolean
}
