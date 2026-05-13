<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Bar, Doughnut, Line } from 'vue-chartjs'
import { ArcElement, BarElement, CategoryScale, Chart as ChartJS, Legend, LinearScale, LineElement, PointElement, Tooltip } from 'chart.js'
import * as reportApi from '@/api/reports'
import * as savedViewApi from '@/api/savedViews'
import * as userApi from '@/api/users'
import * as templateApi from '@/api/templates'
import { useUiStore } from '@/stores/ui'
import type { FollowupTemplateResponse, ReportFilters, ReportsOverviewResponse, SavedViewResponse, UserResponse } from '@/types/api'
import { titleCase } from '@/utils/formatters'

ChartJS.register(ArcElement, BarElement, CategoryScale, Legend, LinearScale, Tooltip, LineElement, PointElement)

const loading = ref(false)
const uiStore = useUiStore()
const overview = ref<ReportsOverviewResponse | null>(null)
const users = ref<UserResponse[]>([])
const templates = ref<FollowupTemplateResponse[]>([])
const savedViews = ref<SavedViewResponse[]>([])
const viewName = ref('')
const filters = reactive<ReportFilters>({
  dateFrom: '',
  dateTo: '',
  repUserId: null,
  leadStatus: '',
  followupOutcome: '',
  source: '',
  templateId: null,
  channel: '',
  priority: '',
  escalated: null,
  sortBy: 'completedFollowups',
  sortDirection: 'DESC',
})

const funnelChart = computed(() => ({
  labels: Object.keys(overview.value?.funnel ?? {}).map(titleCase),
  datasets: [{ data: Object.values(overview.value?.funnel ?? {}), backgroundColor: ['#0f6c5c', '#1d4ed8', '#d97706', '#16a34a', '#dc2626', '#6b7280'] }],
}))

const repChart = computed(() => ({
  labels: overview.value?.reps.map((rep) => rep.userName) ?? [],
  datasets: [
    { label: 'Assigned Leads', data: overview.value?.reps.map((rep) => rep.assignedLeads) ?? [], backgroundColor: '#0f6c5c' },
    { label: 'Completed Follow-ups', data: overview.value?.reps.map((rep) => rep.completedFollowups) ?? [], backgroundColor: '#d97706' },
  ],
}))

const trendChart = computed(() => ({
  labels: overview.value?.trends.map((trend) => trend.date) ?? [],
  datasets: [
    {
      label: 'Leads Created',
      data: overview.value?.trends.map((trend) => trend.leadsCreated) ?? [],
      borderColor: '#1d4ed8',
      backgroundColor: '#1d4ed8',
      tension: 0.35,
    },
    {
      label: 'Follow-ups Completed',
      data: overview.value?.trends.map((trend) => trend.followupsCompleted) ?? [],
      borderColor: '#0f6c5c',
      backgroundColor: '#0f6c5c',
      tension: 0.35,
    },
  ],
}))

const outcomeChart = computed(() => ({
  labels: Object.keys(overview.value?.outcomes.outcomes ?? {}).map(titleCase),
  datasets: [
    {
      label: 'Outcome Count',
      data: Object.values(overview.value?.outcomes.outcomes ?? {}),
      backgroundColor: ['#0f6c5c', '#1d4ed8', '#d97706', '#ef6c00', '#16a34a', '#14532d', '#dc2626'],
    },
  ],
}))

const sortFields = ['completedFollowups', 'assignedLeads', 'pendingFollowups', 'userName']
const repTableHeaders = [
  { title: 'Rep ID', key: 'userId' },
  { title: 'Rep', key: 'userName' },
  { title: 'Assigned Leads', key: 'assignedLeads' },
  { title: 'Pending Follow-ups', key: 'pendingFollowups' },
  { title: 'Completed Follow-ups', key: 'completedFollowups' },
]

function normalizeFilters() {
  return {
    ...filters,
    dateFrom: filters.dateFrom || undefined,
    dateTo: filters.dateTo || undefined,
    leadStatus: filters.leadStatus || undefined,
    followupOutcome: filters.followupOutcome || undefined,
    source: filters.source || undefined,
    channel: filters.channel || undefined,
    priority: filters.priority || undefined,
  }
}

async function load() {
  loading.value = true
  try {
    overview.value = await reportApi.getReportsOverview(normalizeFilters())
    users.value = await userApi.listUsers().catch(() => [])
    templates.value = await templateApi.listTemplates().catch(() => [])
    savedViews.value = await savedViewApi.listSavedViews('reports').catch(() => [])
  } finally {
    loading.value = false
  }
}

async function saveCurrentView() {
  if (!viewName.value.trim()) return
  await savedViewApi.saveView({
    pageKey: 'reports',
    name: viewName.value.trim(),
    shared: false,
    configJson: JSON.stringify(normalizeFilters()),
  })
  viewName.value = ''
  uiStore.showSuccess('Report view saved')
  await load()
}

async function applySavedView(view: SavedViewResponse) {
  Object.assign(filters, JSON.parse(view.configJson))
  await load()
}

async function exportCsv() {
  await reportApi.exportReportCsv(normalizeFilters())
  uiStore.showSuccess('CSV export generated from current report filters')
}

watch(
  () => [
    filters.dateFrom, filters.dateTo, filters.repUserId, filters.leadStatus, filters.followupOutcome,
    filters.source, filters.templateId, filters.channel, filters.priority, filters.escalated, filters.sortBy, filters.sortDirection,
  ],
  load,
)

onMounted(async () => {
  const today = new Date()
  const start = new Date()
  start.setDate(today.getDate() - 30)
  filters.dateTo = today.toISOString().slice(0, 10)
  filters.dateFrom = start.toISOString().slice(0, 10)
  await load()
})
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">Reporting</h1>
        <p class="page-subtitle">Comprehensive filtered analytics for funnel health, outcomes, and team performance.</p>
      </div>
      <div class="d-flex ga-2">
        <v-text-field v-model="viewName" label="Save view as" hide-details density="comfortable" min-width="220" />
        <v-btn color="primary" variant="tonal" @click="saveCurrentView">Save view</v-btn>
        <v-btn color="secondary" variant="tonal" @click="exportCsv">Export CSV</v-btn>
      </div>
    </div>

    <div class="d-flex ga-2 mb-4 flex-wrap">
      <v-chip v-for="view in savedViews" :key="view.id" variant="tonal" @click="applySavedView(view)">
        {{ view.name }}
      </v-chip>
    </div>

    <v-card class="mb-4">
      <v-card-text>
        <v-row>
          <v-col cols="12" md="2"><v-text-field v-model="filters.dateFrom" label="Date from" type="date" /></v-col>
          <v-col cols="12" md="2"><v-text-field v-model="filters.dateTo" label="Date to" type="date" /></v-col>
          <v-col cols="12" md="2"><v-select v-model="filters.repUserId" label="Rep" :items="users" item-title="fullName" item-value="id" clearable /></v-col>
          <v-col cols="12" md="2"><v-select v-model="filters.templateId" label="Template" :items="templates" item-title="name" item-value="id" clearable /></v-col>
          <v-col cols="12" md="2"><v-select v-model="filters.leadStatus" label="Lead status" :items="['', 'NEW', 'IN_PROGRESS', 'QUALIFIED', 'WON', 'LOST', 'DORMANT']" /></v-col>
          <v-col cols="12" md="2"><v-select v-model="filters.followupOutcome" label="Outcome" :items="['', 'NO_RESPONSE', 'INTERESTED', 'NOT_INTERESTED', 'CALLBACK_REQUESTED', 'QUALIFIED', 'WON', 'LOST']" /></v-col>
          <v-col cols="12" md="2"><v-select v-model="filters.channel" label="Channel" :items="['', 'CALL', 'EMAIL', 'WHATSAPP', 'LINKEDIN', 'MEETING']" /></v-col>
          <v-col cols="12" md="2"><v-select v-model="filters.priority" label="Priority" :items="['', 'LOW', 'MEDIUM', 'HIGH']" /></v-col>
          <v-col cols="12" md="3"><v-text-field v-model="filters.source" label="Source" /></v-col>
          <v-col cols="12" md="3"><v-select v-model="filters.escalated" label="Escalated" :items="[{ title: 'All', value: null }, { title: 'Yes', value: true }, { title: 'No', value: false }]" /></v-col>
          <v-col cols="12" md="3"><v-select v-model="filters.sortBy" label="Sort by" :items="sortFields" /></v-col>
          <v-col cols="12" md="3"><v-select v-model="filters.sortDirection" label="Sort direction" :items="['ASC', 'DESC']" /></v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <v-row class="mb-4">
      <v-col cols="12" md="3">
        <v-card class="pa-5">
          <div class="text-overline">Leads In Range</div>
          <div class="text-h4">{{ overview?.summary.leadsInRange ?? 0 }}</div>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <v-card class="pa-5">
          <div class="text-overline">Completed Follow-ups</div>
          <div class="text-h4">{{ overview?.summary.followupsCompleted ?? 0 }}</div>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <v-card class="pa-5">
          <div class="text-overline">Overdue Follow-ups</div>
          <div class="text-h4 text-error">{{ overview?.summary.overdueFollowups ?? 0 }}</div>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <v-card class="pa-5">
          <div class="text-overline">Completion Rate</div>
          <div class="text-h4 text-primary">{{ overview?.summary.completionRate ?? 0 }}%</div>
        </v-card>
      </v-col>
    </v-row>

    <v-row>
      <v-col cols="12" md="4">
        <v-card class="pa-4">
          <div class="text-h6 mb-4">Lead Funnel</div>
          <Doughnut :data="funnelChart" />
        </v-card>
      </v-col>
      <v-col cols="12" md="8">
        <v-card class="pa-4">
          <div class="text-h6 mb-4">Rep Performance</div>
          <Bar :data="repChart" />
        </v-card>
      </v-col>
    </v-row>

    <v-row class="mt-4">
      <v-col cols="12" md="6">
        <v-card class="pa-4">
          <div class="text-h6 mb-4">Outcome Breakdown</div>
          <Bar :data="outcomeChart" />
          <div class="text-caption mt-3 text-medium-emphasis">
            Unknown legacy outcomes: {{ overview?.outcomes.unknownOutcomeCount ?? 0 }}
          </div>
        </v-card>
      </v-col>
      <v-col cols="12" md="6">
        <v-card class="pa-4">
          <div class="text-h6 mb-4">Trend (Leads vs Completed Follow-ups)</div>
          <Line :data="trendChart" />
        </v-card>
      </v-col>
    </v-row>

    <v-card class="mt-4">
      <v-card-title>Rep Performance Table</v-card-title>
      <v-data-table :headers="repTableHeaders" :items="overview?.reps ?? []" :loading="loading" />
    </v-card>
  </div>
</template>
