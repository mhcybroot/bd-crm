<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as leadApi from '@/api/leads'
import * as templateApi from '@/api/templates'
import * as userApi from '@/api/users'
import AppEmptyState from '@/components/common/AppEmptyState.vue'
import { useUiStore } from '@/stores/ui'
import type {
  FollowupTemplateResponse,
  LeadPriority,
  LeadStatus,
  LeadSummaryResponse,
  PipelineBoardColumnResponse,
  PipelineBoardResponse,
  PipelineStageLeadPageResponse,
  UserResponse,
} from '@/types/api'

const router = useRouter()
const uiStore = useUiStore()

const templates = ref<FollowupTemplateResponse[]>([])
const users = ref<UserResponse[]>([])
const templateId = ref<number | null>(null)
const board = ref<PipelineBoardResponse | null>(null)
const boardLoading = ref(false)
const stageDrawer = ref(false)
const stageLoading = ref(false)
const selectedStage = ref<PipelineBoardColumnResponse | null>(null)
const stagePage = ref<PipelineStageLeadPageResponse | null>(null)
const draggingLeadId = ref<number | null>(null)
const dragOverStageId = ref<number | null>(null)

const filters = reactive({
  search: '',
  assignedUserId: null as number | null,
  priority: '' as LeadPriority | '',
  leadStatus: '' as LeadStatus | '',
  source: '',
  dateFrom: '',
  dateTo: '',
})

const statusOptions: Array<LeadStatus | ''> = ['', 'NEW', 'IN_PROGRESS', 'QUALIFIED', 'WON', 'LOST', 'DORMANT']
const priorityOptions: Array<LeadPriority | ''> = ['', 'LOW', 'MEDIUM', 'HIGH']

async function loadTemplates() {
  templates.value = await templateApi.listTemplates().catch(() => [])
  templateId.value = templateId.value ?? templates.value[0]?.id ?? null
}

async function loadUsers() {
  users.value = await userApi.listUsers().catch(() => [])
}

function filterParams(extra: Record<string, unknown> = {}) {
  return {
    search: filters.search || undefined,
    assignedUserId: filters.assignedUserId ?? undefined,
    priority: filters.priority || undefined,
    leadStatus: filters.leadStatus || undefined,
    source: filters.source || undefined,
    dateFrom: filters.dateFrom || undefined,
    dateTo: filters.dateTo || undefined,
    ...extra,
  }
}

async function loadBoard() {
  if (!templateId.value) return
  boardLoading.value = true
  try {
    board.value = await templateApi.getTemplateBoard(templateId.value, filterParams())
    if (selectedStage.value) {
      const refreshed = board.value.columns.find((column) => column.stageId === selectedStage.value?.stageId) ?? null
      selectedStage.value = refreshed
      if (!refreshed) {
        stageDrawer.value = false
        stagePage.value = null
      }
    }
  } finally {
    boardLoading.value = false
  }
}

async function openStage(column: PipelineBoardColumnResponse, page = 0) {
  if (!templateId.value) return
  selectedStage.value = column
  stageDrawer.value = true
  stageLoading.value = true
  try {
    stagePage.value = await templateApi.getTemplateBoardStageLeads(templateId.value, column.stageId, filterParams({ page, size: 25 }))
  } finally {
    stageLoading.value = false
  }
}

async function refreshAfterMove(nextStageId?: number) {
  await loadBoard()
  if (stageDrawer.value && selectedStage.value) {
    const stageToOpen =
      board.value?.columns.find((column) => column.stageId === (nextStageId ?? selectedStage.value?.stageId)) ??
      board.value?.columns.find((column) => column.stageId === selectedStage.value?.stageId)
    if (stageToOpen) {
      await openStage(stageToOpen, stagePage.value?.page ?? 0)
    }
  }
}

async function moveLead(lead: LeadSummaryResponse, stageId: number) {
  try {
    await leadApi.updateLeadStage(lead.id, { stageId, note: 'Moved from pipeline board' })
    uiStore.showSuccess('Lead moved to new stage')
    await refreshAfterMove(stageId)
  } catch (error) {
    uiStore.showError(error instanceof Error ? error.message : 'Unable to move lead')
  } finally {
    draggingLeadId.value = null
    dragOverStageId.value = null
  }
}

function startDrag(leadId: number) {
  draggingLeadId.value = leadId
}

function clearDragState() {
  draggingLeadId.value = null
  dragOverStageId.value = null
}

async function dropOnStage(stageId: number) {
  if (!draggingLeadId.value || !stagePage.value) return
  const lead = stagePage.value.content.find((item) => item.id === draggingLeadId.value)
  if (!lead) return
  await moveLead(lead, stageId)
}

async function changeStagePage(pageNumber: number) {
  if (!selectedStage.value) return
  await openStage(selectedStage.value, pageNumber - 1)
}

let debounceHandle: number | undefined
watch(
  () => filters.search,
  () => {
    window.clearTimeout(debounceHandle)
    debounceHandle = window.setTimeout(async () => {
      await loadBoard()
      if (selectedStage.value) {
        await openStage(selectedStage.value, 0)
      }
    }, 300)
  },
)

watch(
  () => [templateId.value, filters.assignedUserId, filters.priority, filters.leadStatus, filters.source, filters.dateFrom, filters.dateTo],
  async () => {
    await loadBoard()
    if (selectedStage.value) {
      const current = board.value?.columns.find((column) => column.stageId === selectedStage.value?.stageId)
      if (current) {
        await openStage(current, 0)
      }
    }
  },
)

onMounted(async () => {
  await Promise.all([loadTemplates(), loadUsers()])
  await loadBoard()
})
</script>

<template>
  <div class="page-shell">
    <div class="page-header page-hero">
      <div>
        <h1 class="page-title">Pipeline Board</h1>
        <p class="page-subtitle">Track large pipelines by stage, SLA, and owner without loading every lead card at once.</p>
      </div>
      <v-select
        v-model="templateId"
        label="Template"
        :items="templates"
        item-title="name"
        item-value="id"
        min-width="260"
      />
    </div>

    <v-card class="mb-4 crm-card">
      <v-card-text>
        <v-row>
          <v-col cols="12" md="3">
            <v-text-field v-model="filters.search" label="Search leads" prepend-inner-icon="mdi-magnify" />
          </v-col>
          <v-col cols="12" md="2">
            <v-select
              v-model="filters.assignedUserId"
              :items="users"
              item-title="fullName"
              item-value="id"
              label="Owner"
              clearable
            />
          </v-col>
          <v-col cols="12" md="2">
            <v-select v-model="filters.priority" :items="priorityOptions" label="Priority" />
          </v-col>
          <v-col cols="12" md="2">
            <v-select v-model="filters.leadStatus" :items="statusOptions" label="Lead status" />
          </v-col>
          <v-col cols="12" md="3">
            <v-text-field v-model="filters.source" label="Source" />
          </v-col>
          <v-col cols="12" md="3">
            <v-text-field v-model="filters.dateFrom" type="date" label="Created from" />
          </v-col>
          <v-col cols="12" md="3">
            <v-text-field v-model="filters.dateTo" type="date" label="Created to" />
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <v-row>
      <v-col
        v-for="column in board?.columns ?? []"
        :key="column.stageId"
        cols="12"
        sm="6"
        lg="4"
        xl="3"
      >
        <v-card
          class="fill-height crm-card crm-board-column"
          :class="{ 'pipeline-drop-active': dragOverStageId === column.stageId }"
          @dragover.prevent="dragOverStageId = column.stageId"
          @dragleave="dragOverStageId = null"
          @drop.prevent="dropOnStage(column.stageId)"
        >
          <v-card-text class="d-flex flex-column ga-4">
            <div class="d-flex justify-space-between align-start">
              <div>
                <div class="section-heading">{{ column.stageName }}</div>
                <div class="text-caption text-medium-emphasis">SLA {{ column.slaHours }}h</div>
              </div>
              <v-chip size="small" color="primary" variant="tonal">{{ column.leadCount }}</v-chip>
            </div>

            <div class="d-flex ga-3">
              <v-sheet rounded class="pa-3 border flex-1-1 workflow-kpi">
                <div class="metric-label">Leads</div>
                <div class="text-h6">{{ column.leadCount }}</div>
              </v-sheet>
              <v-sheet rounded class="pa-3 border flex-1-1 workflow-kpi">
                <div class="metric-label">SLA breach</div>
                <div class="text-h6">{{ column.slaBreachCount }}</div>
              </v-sheet>
            </div>

            
            <div class="d-flex justify-space-between align-center">
              <v-btn variant="tonal" color="secondary" @click="openStage(column)">View leads</v-btn>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <div v-if="boardLoading" class="mt-4">
      <v-row>
        <v-col v-for="index in 4" :key="index" cols="12" sm="6" lg="4" xl="3">
          <v-skeleton-loader type="card" />
        </v-col>
      </v-row>
    </div>

    <v-navigation-drawer
      v-model="stageDrawer"
      location="right"
      temporary
      width="520"
      class="stage-drawer"
    >
      <div class="d-flex justify-space-between align-center pa-4 border-b">
        <div>
          <div class="text-h6">{{ selectedStage?.stageName ?? 'Stage leads' }}</div>
          <div class="text-caption text-medium-emphasis">
            {{ stagePage?.totalElements ?? selectedStage?.leadCount ?? 0 }} leads in this filtered view
          </div>
        </div>
        <v-btn icon="mdi-close" variant="text" @click="stageDrawer = false" />
      </div>

      <div class="pa-4 d-flex flex-column ga-4">
        <div v-if="stageLoading">
          <v-skeleton-loader type="list-item-three-line@6" />
        </div>

        <template v-else-if="stagePage && stagePage.content.length">
          <v-card
            v-for="lead in stagePage.content"
            :key="lead.id"
            class="pa-3 crm-card"
            variant="outlined"
            draggable="true"
            @dragstart="startDrag(lead.id)"
            @dragend="clearDragState"
          >
            <div class="d-flex justify-space-between align-start ga-2">
              <div>
                <div class="text-subtitle-1">{{ lead.companyName }}</div>
                <div class="text-body-2 text-medium-emphasis">{{ lead.contactName }}</div>
                <div class="text-caption mt-2">{{ lead.assignedUserName }}</div>
              </div>
              <v-menu>
                <template #activator="{ props }">
                  <v-btn v-bind="props" icon="mdi-dots-vertical" size="small" variant="text" />
                </template>
                <v-list density="compact">
                  <v-list-item @click="router.push(`/leads/${lead.id}`)">
                    <v-list-item-title>Open lead</v-list-item-title>
                  </v-list-item>
                  <v-list-subheader>Move to stage</v-list-subheader>
                  <v-list-item
                    v-for="column in board?.columns ?? []"
                    :key="`${lead.id}-${column.stageId}`"
                    :disabled="column.stageId === lead.currentStageId"
                    @click="moveLead(lead, column.stageId)"
                  >
                    <v-list-item-title>{{ column.stageName }}</v-list-item-title>
                  </v-list-item>
                </v-list>
              </v-menu>
            </div>
          </v-card>

          <div class="d-flex justify-space-between align-center">
            <div class="text-body-2 text-medium-emphasis">
              Page {{ (stagePage.page ?? 0) + 1 }} of {{ Math.max(stagePage.totalPages, 1) }}
            </div>
            <v-pagination
              :model-value="(stagePage.page ?? 0) + 1"
              :length="Math.max(stagePage.totalPages, 1)"
              density="comfortable"
              @update:model-value="changeStagePage"
            />
          </div>
        </template>

        <AppEmptyState
          v-else
          icon="mdi-view-column-outline"
          title="No leads in this stage"
          text="Try adjusting the board filters or open a different stage."
        />
      </div>
    </v-navigation-drawer>
  </div>
</template>

<style scoped>
.pipeline-drop-active {
  outline: 2px dashed rgb(var(--v-theme-primary));
  outline-offset: 4px;
  background: rgba(var(--v-theme-primary), 0.04);
  transform: translateY(-4px) scale(1.01);
}

.border-b {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.flex-1-1 {
  flex: 1 1 0;
}

.stage-drawer {
  background: linear-gradient(180deg, rgba(255, 253, 248, 0.98), rgba(255, 248, 242, 0.94));
  backdrop-filter: blur(18px);
}
</style>
