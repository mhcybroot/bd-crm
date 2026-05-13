<script setup lang="ts">
import { onMounted, ref } from 'vue'
import * as leadApi from '@/api/leads'
import * as templateApi from '@/api/templates'
import { useUiStore } from '@/stores/ui'
import type { FollowupTemplateResponse, PipelineBoardResponse } from '@/types/api'

const uiStore = useUiStore()
const templates = ref<FollowupTemplateResponse[]>([])
const templateId = ref<number | null>(null)
const board = ref<PipelineBoardResponse | null>(null)
const draggingLeadId = ref<number | null>(null)
const dragOverStageId = ref<number | null>(null)

async function loadTemplates() {
  templates.value = await templateApi.listTemplates().catch(() => [])
  templateId.value = templateId.value ?? templates.value[0]?.id ?? null
  if (templateId.value) {
    board.value = await templateApi.getTemplateBoard(templateId.value)
  }
}

async function loadBoard(value: number | null) {
  if (!value) return
  board.value = await templateApi.getTemplateBoard(value)
}

function startDrag(leadId: number) {
  draggingLeadId.value = leadId
}

function clearDragState() {
  draggingLeadId.value = null
  dragOverStageId.value = null
}

async function dropOnStage(stageId: number) {
  if (!draggingLeadId.value) return
  try {
    await leadApi.updateLeadStage(draggingLeadId.value, { stageId, note: 'Moved from pipeline board' })
    await loadBoard(templateId.value)
    uiStore.showSuccess('Lead moved to new stage')
  } catch (error) {
    uiStore.showError(error instanceof Error ? error.message : 'Unable to move lead')
  } finally {
    clearDragState()
  }
}

onMounted(loadTemplates)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">Pipeline Board</h1>
        <p class="page-subtitle">Track leads by stage, stage SLA, and owner in a board view.</p>
      </div>
      <v-select
        v-model="templateId"
        label="Template"
        :items="templates"
        item-title="name"
        item-value="id"
        min-width="220"
        @update:model-value="loadBoard"
      />
    </div>

    <div class="d-flex ga-4 overflow-x-auto pb-4">
      <v-card
        v-for="column in board?.columns ?? []"
        :key="column.stageId"
        min-width="300"
        class="pa-3"
        :class="{ 'pipeline-drop-active': dragOverStageId === column.stageId }"
        @dragover.prevent="dragOverStageId = column.stageId"
        @dragleave="dragOverStageId = null"
        @drop.prevent="dropOnStage(column.stageId)"
      >
        <div class="d-flex justify-space-between align-center mb-3">
          <div>
            <div class="text-h6">{{ column.stageName }}</div>
            <div class="text-caption text-medium-emphasis">SLA {{ column.slaHours }}h</div>
          </div>
          <v-chip size="small" color="primary" variant="tonal">{{ column.leadCount }}</v-chip>
        </div>
        <v-card
          v-for="lead in column.leads"
          :key="lead.id"
          class="mb-3 pa-3"
          variant="outlined"
          draggable="true"
          @dragstart="startDrag(lead.id)"
          @dragend="clearDragState"
        >
          <div class="text-subtitle-1">{{ lead.companyName }}</div>
          <div class="text-body-2 text-medium-emphasis">{{ lead.contactName }}</div>
          <div class="text-caption mt-2">{{ lead.assignedUserName }}</div>
        </v-card>
      </v-card>
    </div>
  </div>
</template>

<style scoped>
.pipeline-drop-active {
  outline: 2px dashed rgb(var(--v-theme-primary));
  outline-offset: 4px;
  background: rgba(var(--v-theme-primary), 0.04);
}
</style>
