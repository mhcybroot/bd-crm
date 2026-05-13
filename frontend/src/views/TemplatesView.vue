<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import * as templateApi from '@/api/templates'
import { useUiStore } from '@/stores/ui'
import type { ContactChannel, FollowupTemplateRequest, FollowupTemplateResponse } from '@/types/api'

const uiStore = useUiStore()
const templates = ref<FollowupTemplateResponse[]>([])
const dialog = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<FollowupTemplateRequest>({
  name: '',
  description: '',
  isDefault: false,
  active: true,
  steps: [{ stepNumber: 1, dayOffset: 0, channel: 'CALL', instructions: '' }],
  stages: [
    { name: 'Prospecting', stageOrder: 1, slaHours: 48, exitAutomation: 'START_FIRST_TOUCH' },
    { name: 'Contacted', stageOrder: 2, slaHours: 72, exitAutomation: 'FOLLOWUP_REMINDER' },
    { name: 'Qualified', stageOrder: 3, slaHours: 72, exitAutomation: 'RECOMMEND_MEETING' },
  ],
})

async function load() {
  templates.value = await templateApi.listTemplates()
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    name: '',
    description: '',
    isDefault: false,
    active: true,
    steps: [{ stepNumber: 1, dayOffset: 0, channel: 'CALL' as ContactChannel, instructions: '' }],
    stages: [
      { name: 'Prospecting', stageOrder: 1, slaHours: 48, exitAutomation: 'START_FIRST_TOUCH' },
      { name: 'Contacted', stageOrder: 2, slaHours: 72, exitAutomation: 'FOLLOWUP_REMINDER' },
      { name: 'Qualified', stageOrder: 3, slaHours: 72, exitAutomation: 'RECOMMEND_MEETING' },
    ],
  })
}

function normalizeStages(template: FollowupTemplateResponse) {
  if (template.stages?.length) return template.stages
  return [
    { id: 0, name: 'Prospecting', stageOrder: 1, slaHours: 48, exitAutomation: 'START_FIRST_TOUCH' },
    { id: 0, name: 'Contacted', stageOrder: 2, slaHours: 72, exitAutomation: 'FOLLOWUP_REMINDER' },
    { id: 0, name: 'Qualified', stageOrder: 3, slaHours: 72, exitAutomation: 'RECOMMEND_MEETING' },
  ]
}

function editTemplate(template: FollowupTemplateResponse) {
  editingId.value = template.id
  Object.assign(form, {
    name: template.name,
    description: template.description || '',
    isDefault: template.isDefault,
    active: template.active,
    steps: template.steps.map((step) => ({
      stepNumber: step.stepNumber,
      dayOffset: step.dayOffset,
      channel: step.channel,
      instructions: step.instructions || '',
    })),
    stages: normalizeStages(template).map((stage) => ({
      name: stage.name,
      stageOrder: stage.stageOrder,
      slaHours: stage.slaHours,
      exitAutomation: stage.exitAutomation || '',
    })),
  })
  dialog.value = true
}

function addStep() {
  if (form.steps.length >= 7) return
  form.steps.push({
    stepNumber: form.steps.length + 1,
    dayOffset: 0,
    channel: 'CALL',
    instructions: '',
  })
}

function removeStep(index: number) {
  form.steps.splice(index, 1)
  form.steps.forEach((step, position) => { step.stepNumber = position + 1 })
}

function addStage() {
  form.stages.push({
    name: `Stage ${form.stages.length + 1}`,
    stageOrder: form.stages.length + 1,
    slaHours: 72,
    exitAutomation: '',
  })
}

function removeStage(index: number) {
  form.stages.splice(index, 1)
  form.stages.forEach((stage, position) => { stage.stageOrder = position + 1 })
}

async function submit() {
  try {
    if (editingId.value) {
      await templateApi.updateTemplate(editingId.value, form)
    } else {
      await templateApi.createTemplate(form)
    }
    dialog.value = false
    resetForm()
    uiStore.showSuccess('Template saved')
    await load()
  } catch (error) {
    uiStore.showError(error instanceof Error ? error.message : 'Unable to save template')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">Follow-up Templates</h1>
        <p class="page-subtitle">Design the 7-touch cadence each lead follows after assignment.</p>
      </div>
      <v-btn color="primary" prepend-icon="mdi-plus" @click="dialog = true; resetForm()">New template</v-btn>
    </div>

    <v-row>
      <v-col v-for="template in templates" :key="template.id" cols="12" md="6">
        <v-card class="pa-4 h-100">
          <div class="d-flex justify-space-between align-start mb-3">
            <div>
              <div class="text-h6">{{ template.name }}</div>
              <div class="text-body-2 text-medium-emphasis">{{ template.description }}</div>
            </div>
            <div class="d-flex ga-2">
              <v-chip v-if="template.isDefault" color="primary" variant="tonal" size="small">Default</v-chip>
              <v-chip :color="template.active ? 'success' : 'warning'" variant="tonal" size="small">
                {{ template.active ? 'Active' : 'Inactive' }}
              </v-chip>
            </div>
          </div>
          <v-table density="compact">
            <thead>
              <tr>
                <th>Step</th>
                <th>Offset</th>
                <th>Channel</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="step in template.steps" :key="step.id">
                <td>{{ step.stepNumber }}</td>
                <td>{{ step.dayOffset }} day(s)</td>
                <td>{{ step.channel }}</td>
              </tr>
            </tbody>
          </v-table>
          <div class="mt-4">
            <div class="text-subtitle-2 mb-2">Pipeline stages</div>
            <v-chip
              v-for="stage in normalizeStages(template)"
              :key="`${template.id}-${stage.stageOrder}`"
              size="small"
              variant="tonal"
              class="mr-2 mb-2"
            >
              {{ stage.stageOrder }}. {{ stage.name }}
            </v-chip>
          </div>
          <div class="mt-4">
            <v-btn variant="outlined" @click="editTemplate(template)">Edit</v-btn>
          </div>
        </v-card>
      </v-col>
    </v-row>

    <v-dialog v-model="dialog" max-width="860">
      <v-card>
        <v-card-title>{{ editingId ? 'Edit template' : 'New template' }}</v-card-title>
        <v-card-text>
          <v-row>
            <v-col cols="12" md="6"><v-text-field v-model="form.name" label="Template name" /></v-col>
            <v-col cols="12" md="6"><v-text-field v-model="form.description" label="Description" /></v-col>
            <v-col cols="12" md="6"><v-switch v-model="form.isDefault" label="Default template" /></v-col>
            <v-col cols="12" md="6"><v-switch v-model="form.active" label="Active" /></v-col>
          </v-row>
          <div class="d-flex justify-space-between align-center mb-3">
            <div class="text-subtitle-1">Template steps</div>
            <v-btn :disabled="form.steps.length >= 7" variant="text" color="primary" @click="addStep">Add step</v-btn>
          </div>
          <v-alert type="info" variant="tonal" class="mb-4">Templates can include a maximum of 7 steps.</v-alert>
          <v-row v-for="(step, index) in form.steps" :key="index" class="mb-1">
            <v-col cols="12" md="2"><v-text-field v-model.number="step.stepNumber" label="Step" type="number" disabled /></v-col>
            <v-col cols="12" md="3"><v-text-field v-model.number="step.dayOffset" label="Offset" type="number" /></v-col>
            <v-col cols="12" md="3"><v-select v-model="step.channel" label="Channel" :items="['CALL', 'EMAIL', 'WHATSAPP', 'LINKEDIN', 'MEETING']" /></v-col>
            <v-col cols="12" md="3"><v-text-field v-model="step.instructions" label="Instructions" /></v-col>
            <v-col cols="12" md="1" class="d-flex align-center">
              <v-btn icon="mdi-delete-outline" variant="text" :disabled="form.steps.length === 1" @click="removeStep(index)" />
            </v-col>
          </v-row>
          <div class="d-flex justify-space-between align-center mt-6 mb-3">
            <div class="text-subtitle-1">Pipeline stages</div>
            <v-btn variant="text" color="primary" @click="addStage">Add stage</v-btn>
          </div>
          <v-row v-for="(stage, index) in form.stages" :key="`stage-${index}`" class="mb-1">
            <v-col cols="12" md="3"><v-text-field v-model="stage.name" label="Stage name" /></v-col>
            <v-col cols="12" md="2"><v-text-field v-model.number="stage.stageOrder" label="Order" type="number" disabled /></v-col>
            <v-col cols="12" md="3"><v-text-field v-model.number="stage.slaHours" label="SLA hours" type="number" /></v-col>
            <v-col cols="12" md="3"><v-text-field v-model="stage.exitAutomation" label="Automation hook" /></v-col>
            <v-col cols="12" md="1" class="d-flex align-center">
              <v-btn icon="mdi-delete-outline" variant="text" :disabled="form.stages.length === 1" @click="removeStage(index)" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialog = false">Cancel</v-btn>
          <v-btn color="primary" @click="submit">Save</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>
