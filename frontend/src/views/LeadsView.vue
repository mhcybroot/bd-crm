<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as leadApi from '@/api/leads'
import * as userApi from '@/api/users'
import AppEmptyState from '@/components/common/AppEmptyState.vue'
import StatusChip from '@/components/common/StatusChip.vue'
import type { LeadStatus, LeadSummaryResponse, UserResponse } from '@/types/api'

const router = useRouter()
const loading = ref(true)
const leads = ref<LeadSummaryResponse[]>([])
const users = ref<UserResponse[]>([])
const totalPages = ref(0)
const totalElements = ref(0)
const filters = reactive({
  page: 1,
  size: 10,
  status: '' as LeadStatus | '',
  assignedUserId: null as number | null,
  search: '',
})

async function loadUsers() {
  try {
    users.value = await userApi.listUsers()
  } catch {
    users.value = []
  }
}

async function loadLeads() {
  loading.value = true
  try {
    const response = await leadApi.listLeads({
      page: filters.page - 1,
      size: filters.size,
      status: filters.status,
      assignedUserId: filters.assignedUserId,
      search: filters.search,
    })
    leads.value = response.content
    totalPages.value = response.totalPages
    totalElements.value = response.totalElements
  } finally {
    loading.value = false
  }
}

watch(() => [filters.page, filters.size, filters.status, filters.assignedUserId], loadLeads)

let debounceHandle: number | undefined
watch(
  () => filters.search,
  () => {
    window.clearTimeout(debounceHandle)
    debounceHandle = window.setTimeout(loadLeads, 300)
  },
)

onMounted(async () => {
  await Promise.all([loadUsers(), loadLeads()])
})
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">Leads</h1>
        <p class="page-subtitle">Search, filter, and manage the pipeline from first contact to close.</p>
      </div>
      <v-btn color="primary" prepend-icon="mdi-plus" @click="router.push('/leads/new')">New lead</v-btn>
    </div>

    <v-card class="mb-4">
      <v-card-text>
        <v-row>
          <v-col cols="12" md="4">
            <v-text-field v-model="filters.search" label="Search leads" prepend-inner-icon="mdi-magnify" />
          </v-col>
          <v-col cols="12" md="3">
            <v-select
              v-model="filters.status"
              label="Status"
              :items="['', 'NEW', 'IN_PROGRESS', 'QUALIFIED', 'WON', 'LOST', 'DORMANT']"
            />
          </v-col>
          <v-col cols="12" md="3">
            <v-select
              v-model="filters.assignedUserId"
              label="Assignee"
              item-title="fullName"
              item-value="id"
              :items="users"
              clearable
            />
          </v-col>
          <v-col cols="12" md="2">
            <v-select v-model="filters.size" label="Page size" :items="[10, 20, 50]" />
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <v-card>
      <v-data-table :items="leads" :loading="loading" :items-per-page="filters.size">
        <template #headers>
          <tr>
            <th class="text-left">Company</th>
            <th class="text-left">Contact</th>
            <th class="text-left">Status</th>
            <th class="text-left">Priority</th>
            <th class="text-left">Assignee</th>
            <th class="text-left">Template</th>
          </tr>
        </template>
        <template #item="{ item }">
          <tr class="cursor-pointer" @click="router.push(`/leads/${item.id}`)">
            <td>{{ item.companyName }}</td>
            <td>{{ item.contactName }}</td>
            <td><StatusChip :value="item.status" /></td>
            <td><StatusChip :value="item.priority" /></td>
            <td>{{ item.assignedUserName }}</td>
            <td>{{ item.templateName }}</td>
          </tr>
        </template>
        <template #bottom>
          <div class="d-flex justify-space-between align-center pa-4">
            <div class="text-body-2 text-medium-emphasis">{{ totalElements }} leads</div>
            <v-pagination v-model="filters.page" :length="Math.max(totalPages, 1)" density="comfortable" />
          </div>
        </template>
        <template #no-data>
          <div class="pa-6">
            <AppEmptyState
              icon="mdi-domain-off"
              title="No leads found"
              text="Try adjusting your filters or create a new lead to begin tracking outreach."
            />
          </div>
        </template>
      </v-data-table>
    </v-card>
  </div>
</template>
