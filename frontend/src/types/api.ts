export type RoleName = 'ADMIN' | 'MANAGER' | 'REP'
export type LeadStatus = 'NEW' | 'IN_PROGRESS' | 'QUALIFIED' | 'WON' | 'LOST' | 'DORMANT'
export type LeadPriority = 'LOW' | 'MEDIUM' | 'HIGH'
export type FollowupStatus = 'DUE' | 'OVERDUE' | 'COMPLETED' | 'SKIPPED' | 'CANCELLED'
export type ContactChannel = 'CALL' | 'EMAIL' | 'WHATSAPP' | 'LINKEDIN' | 'MEETING'
export type DuplicateState = 'CLEAR' | 'SUSPECTED' | 'REVIEWED' | 'MERGED'
export type DocumentStatus = 'DRAFT' | 'SENT' | 'VIEWED' | 'SIGNED' | 'ARCHIVED'
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
  currentStageId: number | null
  currentStageName: string | null
  duplicateState: DuplicateState
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

export interface LeadQualificationResponse {
  id: number
  budgetRange: string | null
  authorityLevel: string | null
  needSummary: string | null
  timelineTarget: string | null
  fitScore: number
  engagementScore: number
  totalScore: number
  qualificationNotes: string | null
  updatedByUserId: number | null
  updatedByUserName: string | null
  qualificationUpdatedAt: string | null
}

export interface LeadScoreSummaryResponse {
  fitScore: number
  engagementScore: number
  totalScore: number
}

export interface LeadStageHistoryResponse {
  id: number
  stageId: number
  stageName: string
  changedByUserId: number
  changedByUserName: string
  changeNote: string | null
  enteredAt: string
  exitedAt: string | null
}

export interface LeadCommunicationResponse {
  id: number
  channel: ContactChannel
  subject: string | null
  body: string | null
  outcome: string | null
  occurredAt: string
  actorId: number
  actorName: string
}

export interface AttachmentUploadResponse {
  id: number
  originalFileName: string
  contentType: string | null
  fileSize: number
  checksum: string
  uploadedByUserId: number
  uploadedByUserName: string
  createdAt: string
}

export interface DocumentLifecycleResponse {
  id: number
  attachmentId: number
  title: string
  status: DocumentStatus
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
  qualification: LeadQualificationResponse
  score: LeadScoreSummaryResponse
  stageHistory: LeadStageHistoryResponse[]
  communications: LeadCommunicationResponse[]
  attachments: AttachmentUploadResponse[]
  documents: DocumentLifecycleResponse[]
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

export interface StageDefinitionResponse {
  id: number
  name: string
  stageOrder: number
  slaHours: number
  exitAutomation: string | null
}

export interface FollowupTemplateResponse {
  id: number
  name: string
  description: string | null
  isDefault: boolean
  active: boolean
  steps: TemplateStepResponse[]
  stages: StageDefinitionResponse[]
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

export interface PipelineBoardColumnResponse {
  stageId: number
  stageName: string
  slaHours: number
  leadCount: number
  slaBreachCount: number
}

export interface PipelineBoardFilters {
  search?: string
  assignedUserId?: number | null
  priority?: LeadPriority | ''
  leadStatus?: LeadStatus | ''
  source?: string
  dateFrom?: string
  dateTo?: string
}

export interface PipelineBoardResponse {
  templateId: number
  templateName: string
  filtersApplied: PipelineBoardFilters
  columns: PipelineBoardColumnResponse[]
}

export interface PipelineStageLeadPageResponse {
  stageId: number
  stageName: string
  totalElements: number
  totalPages: number
  page: number
  size: number
  content: LeadSummaryResponse[]
}

export interface NotificationResponse {
  id: number
  type: string
  title: string
  message: string
  actionUrl: string | null
  leadId: number | null
  followupId: number | null
  readAt: string | null
  createdAt: string
}

export interface NotificationPreferenceResponse {
  inAppEnabled: boolean
  emailEnabled: boolean
}

export interface DuplicateCandidateResponse {
  id: number
  leadId: number
  leadCompanyName: string
  matchedLeadId: number
  matchedLeadCompanyName: string
  matchScore: number
  state: DuplicateState
  reason: string | null
}

export interface SavedViewResponse {
  id: number
  pageKey: string
  name: string
  shared: boolean
  configJson: string
  ownerUserId: number
  ownerUserName: string
}

export interface SearchItem {
  type: string
  id: number
  leadId: number | null
  title: string
  subtitle: string | null
}

export interface GlobalSearchResponse {
  leads: SearchItem[]
  notes: SearchItem[]
  activities: SearchItem[]
  followups: SearchItem[]
  attachments: SearchItem[]
}

export interface CommandCenterResponse {
  notifications: NotificationResponse[]
  dueFollowups: LeadFollowupResponse[]
  overdueFollowups: LeadFollowupResponse[]
  duplicates: DuplicateCandidateResponse[]
  recommendations: string[]
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

export interface LeadQualificationRequest {
  budgetRange: string | null
  authorityLevel: string | null
  needSummary: string | null
  timelineTarget: string | null
  fitScore: number
  engagementScore: number
  qualificationNotes: string | null
}

export interface LeadCommunicationRequest {
  channel: ContactChannel
  subject: string | null
  body: string | null
  outcome: string | null
  occurredAt: string | null
}

export interface LeadAssignmentRequest {
  assignedUserId: number
}

export interface LeadStatusUpdateRequest {
  status: LeadStatus
}

export interface LeadStageUpdateRequest {
  stageId: number
  note?: string | null
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

export interface StageDefinitionRequest {
  name: string
  stageOrder: number
  slaHours: number
  exitAutomation: string
}

export interface FollowupTemplateRequest {
  name: string
  description: string
  isDefault: boolean
  active: boolean
  steps: TemplateStepRequest[]
  stages: StageDefinitionRequest[]
}

export interface NotificationPreferenceRequest {
  inAppEnabled: boolean
  emailEnabled: boolean
}

export interface DocumentLifecycleRequest {
  title: string
  status: DocumentStatus
}

export interface SavedViewRequest {
  pageKey: string
  name: string
  shared: boolean
  configJson: string
}

export interface LeadMergeRequest {
  sourceLeadId: number
  targetLeadId: number
  summary: string | null
}

export interface BulkLeadActionRequest {
  leadIds: number[]
  assignedUserId?: number | null
  status?: LeadStatus | null
  stageId?: number | null
}

export interface BulkFollowupActionRequest {
  followupIds: number[]
  action: string
  dueDate?: string | null
  assignedUserId?: number | null
  notes?: string | null
}

export type LeadImportMode = 'CREATE_ONLY' | 'UPSERT_BY_EMAIL_OR_PHONE'
export type LeadImportTargetField =
  | 'companyName'
  | 'contactName'
  | 'email'
  | 'phone'
  | 'source'
  | 'description'
  | 'priority'
  | 'assignedUserId'
  | 'templateId'
  | 'IGNORE'

export interface LeadImportTemplateField {
  key: Exclude<LeadImportTargetField, 'IGNORE'>
  label: string
  required: boolean
  formatHint: string
  example: string
}

export interface LeadImportValidationIssue {
  field: string
  message: string
}

export interface LeadImportColumnMapping {
  [header: string]: LeadImportTargetField
}

export interface LeadImportPreviewRequest {
  importMode: LeadImportMode
  columnMappings: LeadImportColumnMapping
}

export interface LeadImportPreviewRow {
  rowNumber: number
  values: Record<string, string | null>
  issues: LeadImportValidationIssue[]
  warnings: string[]
  duplicateSuspected: boolean
  valid: boolean
  suggestedAction: string
}

export interface LeadImportSummaryResponse {
  validRows: number
  warningRows: number
  invalidRows: number
  duplicateRows: number
}

export interface LeadImportPreviewResponse {
  detectedHeaders: string[]
  requiredFields: string[]
  resolvedMapping: Record<string, string>
  totalRows: number
  rows: LeadImportPreviewRow[]
  warnings: string[]
  summary: LeadImportSummaryResponse
  fieldGuide: LeadImportTemplateField[]
}

export interface LeadImportRowResult {
  rowNumber: number
  outcome: string
  messages: string[]
}

export interface LeadImportResultResponse {
  createdCount: number
  updatedCount: number
  skippedCount: number
  duplicateCount: number
  invalidCount: number
  errors: string[]
  rowResults: LeadImportRowResult[]
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
