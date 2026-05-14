<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as exportApi from '@/api/export'
import * as importApi from '@/api/imports'
import * as leadApi from '@/api/leads'
import * as userApi from '@/api/users'
import AppEmptyState from '@/components/common/AppEmptyState.vue'
import LeadImportDialog from '@/components/imports/LeadImportDialog.vue'
import StatusChip from '@/components/common/StatusChip.vue'
import { useUiStore } from '@/stores/ui'
import type { LeadStatus, LeadSummaryResponse, UserResponse } from '@/types/api'

const router = useRouter()
const uiStore = useUiStore()
const loading = ref(true)
const leads = ref<LeadSummaryResponse[]>([])
const users = ref<UserResponse[]>([])
const totalPages = ref(0)
const totalElements = ref(0)
const importDialog = ref(false)
const bulkDialog = ref(false)
const bulk = reactive({
  leadIds: [] as number[],
  assignedUserId: null as number | null,
  status: '' as LeadStatus | '',
})
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

async function exportCsv() {
  await exportApi.exportLeadsCsv()
  uiStore.showSuccess('Lead CSV export generated')
}

async function downloadImportTemplate() {
  const blob = await importApi.downloadLeadImportTemplate()
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = 'lead-import-template.csv'
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  URL.revokeObjectURL(url)
  uiStore.showSuccess('Lead import template downloaded')
}

async function applyBulkAction() {
  await leadApi.bulkLeadAction({
    leadIds: bulk.leadIds,
    assignedUserId: bulk.assignedUserId,
    status: bulk.status || null,
  })
  bulkDialog.value = false
  uiStore.showSuccess('Bulk lead action applied')
  await loadLeads()
}
</script>

<template>
  <div class="page-shell">
    <div class="page-header page-hero">
      <div>
        <h1 class="page-title">Leads</h1>
        <p class="page-subtitle">Search, filter, and manage the pipeline from first contact to close.</p>
      </div>
      <div class="crm-hero-actions">
        <v-btn variant="tonal" color="secondary" prepend-icon="mdi-download" @click="downloadImportTemplate">
          Download template
        </v-btn>
        <v-btn variant="tonal" color="primary" prepend-icon="mdi-file-import" @click="importDialog = true">
          Import leads
        </v-btn>
        <v-btn variant="tonal" color="secondary" @click="exportCsv">Export CSV</v-btn>
        <v-btn variant="tonal" color="primary" @click="bulkDialog = true">Bulk action</v-btn>
        <v-btn color="primary" prepend-icon="mdi-plus" @click="router.push('/leads/new')">New lead</v-btn>
      </div>
    </div>

    <v-card class="mb-4 crm-card">
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

    <v-card class="crm-card">
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
          <tr class="cursor-pointer crm-table-row" @click="router.push(`/leads/${item.id}`)">
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

    <v-dialog v-model="bulkDialog" max-width="720">
      <v-card class="crm-card">
        <v-card-title>Bulk Lead Action</v-card-title>
        <v-card-text>
          <v-select v-model="bulk.leadIds" label="Leads" :items="leads" item-title="companyName" item-value="id" chips multiple />
          <v-select v-model="bulk.assignedUserId" label="Assign to" :items="users" item-title="fullName" item-value="id" clearable />
          <v-select v-model="bulk.status" label="Status" :items="['', 'NEW', 'IN_PROGRESS', 'QUALIFIED', 'WON', 'LOST', 'DORMANT']" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="bulkDialog = false">Cancel</v-btn>
          <v-btn color="primary" @click="applyBulkAction">Apply</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <LeadImportDialog v-model="importDialog" @imported="loadLeads" />
  </div>
</template>
