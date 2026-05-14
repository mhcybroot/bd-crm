<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as dashboardApi from '@/api/dashboard'
import * as userApi from '@/api/users'
import AppEmptyState from '@/components/common/AppEmptyState.vue'
import StatusChip from '@/components/common/StatusChip.vue'
import type { DashboardSummaryResponse, DueFollowupResponse, ReportFilters, UserResponse } from '@/types/api'
import { formatDate, titleCase } from '@/utils/formatters'

const router = useRouter()
const loading = ref(true)
const summary = ref<DashboardSummaryResponse | null>(null)
const workQueue = ref<DueFollowupResponse[]>([])
const users = ref<UserResponse[]>([])
const filters = reactive<ReportFilters>({
  dateFrom: '',
  dateTo: '',
  repUserId: null,
  leadStatus: '',
  followupOutcome: '',
})

const statusCards = computed(() =>
  Object.entries(summary.value?.leadsByStatus ?? {}).map(([key, value]) => ({ key, value })),
)

const outcomeCards = computed(() =>
  Object.entries(summary.value?.topOutcomes ?? {}).map(([key, value]) => ({ key, value })),
)

function toApiFilters() {
  return {
    ...filters,
    dateFrom: filters.dateFrom || undefined,
    dateTo: filters.dateTo || undefined,
    leadStatus: filters.leadStatus || undefined,
    followupOutcome: filters.followupOutcome || undefined,
  }
}

async function load() {
  loading.value = true
  try {
    const [summaryData, queueData] = await Promise.all([
      dashboardApi.getDashboardSummary(toApiFilters()),
      dashboardApi.getWorkQueue(toApiFilters()),
    ])
    summary.value = summaryData
    workQueue.value = queueData
    users.value = await userApi.listUsers().catch(() => [])
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(
  () => [filters.dateFrom, filters.dateTo, filters.leadStatus, filters.followupOutcome, filters.repUserId],
  load,
)
</script>

<template>
  <div class="page-shell">
    <div class="page-header page-hero">
      <div>
        <h1 class="page-title">Dashboard</h1>
        <p class="page-subtitle">Executive visibility with a live operating rhythm across leads, outcomes, and overdue work.</p>
      </div>
      <div class="crm-hero-actions">
        <v-chip color="primary" variant="flat">Live metrics</v-chip>
        <v-chip color="accent" variant="tonal">Team pulse</v-chip>
      </div>
    </div>

    <v-card class="mb-4 crm-card">
      <v-card-text>
        <v-row>
          <v-col cols="12" md="3"><v-text-field v-model="filters.dateFrom" type="date" label="Date from" /></v-col>
          <v-col cols="12" md="3"><v-text-field v-model="filters.dateTo" type="date" label="Date to" /></v-col>
          <v-col cols="12" md="3">
            <v-select
              v-model="filters.repUserId"
              label="Rep"
              :items="users"
              item-title="fullName"
              item-value="id"
              clearable
            />
          </v-col>
          <v-col cols="12" md="3">
            <v-select
              v-model="filters.leadStatus"
              label="Lead status"
              :items="['', 'NEW', 'IN_PROGRESS', 'QUALIFIED', 'WON', 'LOST', 'DORMANT']"
            />
          </v-col>
          <v-col cols="12" md="3">
            <v-select
              v-model="filters.followupOutcome"
              label="Follow-up outcome"
              :items="['', 'NO_RESPONSE', 'INTERESTED', 'NOT_INTERESTED', 'CALLBACK_REQUESTED', 'QUALIFIED', 'WON', 'LOST']"
            />
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <v-row class="mb-4 crm-stagger">
      <v-col cols="12" md="3">
        <v-card class="pa-5 metric-card crm-card">
          <div class="metric-label">Total Leads</div>
          <div class="metric-value">{{ summary?.leadsTotal ?? 0 }}</div>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <v-card class="pa-5 metric-card crm-card">
          <div class="metric-label">New Leads In Range</div>
          <div class="metric-value">{{ summary?.newLeadsInRange ?? 0 }}</div>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <v-card class="pa-5 metric-card crm-card">
          <div class="metric-label">Completed Follow-ups</div>
          <div class="metric-value text-success">{{ summary?.completedFollowups ?? 0 }}</div>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <v-card class="pa-5 metric-card crm-card">
          <div class="metric-label">Completion Rate</div>
          <div class="metric-value text-primary">{{ summary?.completionRate ?? 0 }}%</div>
        </v-card>
      </v-col>
    </v-row>

    <v-row class="mb-4 crm-stagger">
      <v-col v-for="card in statusCards" :key="card.key" cols="6" md="2">
        <v-card class="pa-4 crm-card">
          <div class="metric-label">{{ titleCase(card.key) }}</div>
          <div class="text-h6">{{ card.value }}</div>
        </v-card>
      </v-col>
    </v-row>

    <v-row class="mb-4 crm-stagger">
      <v-col cols="12" md="4">
        <v-card class="pa-5 metric-card crm-card">
          <div class="metric-label">Due Today</div>
          <div class="metric-value">{{ summary?.dueToday ?? 0 }}</div>
        </v-card>
      </v-col>
      <v-col cols="12" md="4">
        <v-card class="pa-5 metric-card crm-card">
          <div class="metric-label">Overdue</div>
          <div class="metric-value text-error">{{ summary?.overdue ?? 0 }}</div>
        </v-card>
      </v-col>
      <v-col cols="12" md="4">
        <v-card class="pa-5 metric-card crm-card">
          <div class="metric-label">Escalated</div>
          <div class="metric-value text-warning">{{ summary?.escalated ?? 0 }}</div>
        </v-card>
      </v-col>
    </v-row>

    <v-card class="mb-4 crm-card">
      <v-card-title>Outcome Summary</v-card-title>
      <v-card-text>
        <v-row class="crm-stagger">
          <v-col v-for="card in outcomeCards" :key="card.key" cols="6" md="3">
            <v-card class="pa-4 crm-card">
              <div class="metric-label">{{ titleCase(card.key) }}</div>
              <div class="text-h6">{{ card.value }}</div>
            </v-card>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <v-card class="crm-card">
      <v-card-title>Due and Overdue Follow-ups</v-card-title>
      <v-data-table :items="workQueue" :loading="loading" items-per-page="8">
        <template #headers>
          <tr>
            <th class="text-left">Lead</th>
            <th class="text-left">Step</th>
            <th class="text-left">Due</th>
            <th class="text-left">Status</th>
            <th class="text-left">Owner</th>
            <th class="text-left">Escalated</th>
          </tr>
        </template>
        <template #item="{ item }">
          <tr class="cursor-pointer crm-table-row" @click="router.push(`/leads/${item.leadId}`)">
            <td>{{ item.companyName }}</td>
            <td>{{ item.stepNumber }}</td>
            <td>{{ formatDate(item.dueDate) }}</td>
            <td><StatusChip :value="item.status" /></td>
            <td>{{ item.assignedUserName }}</td>
            <td>{{ item.escalated ? 'Yes' : 'No' }}</td>
          </tr>
        </template>
        <template #no-data>
          <div class="pa-6">
            <AppEmptyState
              icon="mdi-calendar-check-outline"
              title="No follow-ups pending"
              text="This queue will show due and overdue items that need attention."
            />
          </div>
        </template>
      </v-data-table>
    </v-card>
  </div>
</template>
