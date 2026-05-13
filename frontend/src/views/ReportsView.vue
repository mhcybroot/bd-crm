<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Bar, Doughnut, Line } from 'vue-chartjs'
import { ArcElement, BarElement, CategoryScale, Chart as ChartJS, Legend, LinearScale, LineElement, PointElement, Tooltip } from 'chart.js'
import * as reportApi from '@/api/reports'
import * as userApi from '@/api/users'
import * as templateApi from '@/api/templates'
import type { FollowupTemplateResponse, ReportFilters, ReportsOverviewResponse, UserResponse } from '@/types/api'
import { titleCase } from '@/utils/formatters'

ChartJS.register(ArcElement, BarElement, CategoryScale, Legend, LinearScale, Tooltip, LineElement, PointElement)

const loading = ref(false)
const overview = ref<ReportsOverviewResponse | null>(null)
const users = ref<UserResponse[]>([])
const templates = ref<FollowupTemplateResponse[]>([])
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
  } finally {
    loading.value = false
  }
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
      <v-data-table :items="overview?.reps ?? []" :loading="loading">
        <template #headers>
          <tr>
            <th class="text-left">Rep</th>
            <th class="text-left">Assigned Leads</th>
            <th class="text-left">Pending Follow-ups</th>
            <th class="text-left">Completed Follow-ups</th>
          </tr>
        </template>
      </v-data-table>
    </v-card>
  </div>
</template>
