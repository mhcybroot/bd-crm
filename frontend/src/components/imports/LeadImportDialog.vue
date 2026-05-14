<script setup lang="ts">
import { computed, ref } from 'vue'
import * as importApi from '@/api/imports'
import LeadImportFieldGuide from '@/components/imports/LeadImportFieldGuide.vue'
import LeadImportMappingTable from '@/components/imports/LeadImportMappingTable.vue'
import LeadImportPreviewTable from '@/components/imports/LeadImportPreviewTable.vue'
import { useUiStore } from '@/stores/ui'
import type {
  LeadImportColumnMapping,
  LeadImportMode,
  LeadImportPreviewResponse,
  LeadImportResultResponse,
  LeadImportTemplateField,
  LeadImportTargetField,
} from '@/types/api'

defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'imported'): void
}>()

const uiStore = useUiStore()
const step = ref(1)
const loading = ref(false)
const selectedFile = ref<File | null>(null)
const headers = ref<string[]>([])
const importMode = ref<LeadImportMode>('CREATE_ONLY')
const mapping = ref<LeadImportColumnMapping>({})
const preview = ref<LeadImportPreviewResponse | null>(null)
const result = ref<LeadImportResultResponse | null>(null)

const fieldGuide: LeadImportTemplateField[] = [
  { key: 'companyName', label: 'Company name', required: true, formatHint: 'Text', example: 'Acme Ltd' },
  { key: 'contactName', label: 'Contact name', required: true, formatHint: 'Text', example: 'Amina Rahman' },
  { key: 'email', label: 'Email', required: false, formatHint: 'Valid email', example: 'amina@acme.com' },
  { key: 'phone', label: 'Phone', required: false, formatHint: 'Phone or text', example: '+8801712345678' },
  { key: 'source', label: 'Lead source', required: false, formatHint: 'Text', example: 'LinkedIn' },
  { key: 'description', label: 'Description', required: false, formatHint: 'Free text', example: 'Warm intro from partner' },
  { key: 'priority', label: 'Priority', required: false, formatHint: 'LOW, MEDIUM, HIGH', example: 'HIGH' },
  { key: 'assignedUserId', label: 'Assignee', required: false, formatHint: 'User id, username, or exact email', example: 'bdrep' },
  { key: 'templateId', label: 'Follow-up template', required: false, formatHint: 'Template id or exact template name', example: 'Standard 7 Touch' },
]

const canMoveToPreview = computed(() => {
  const mappedValues = Object.values(mapping.value)
  return mappedValues.includes('companyName') && mappedValues.includes('contactName')
})

const canRunImport = computed(() => {
  return preview.value != null && preview.value.summary.invalidRows < preview.value.totalRows
})

function updateModelValue(value: boolean) {
  emit('update:modelValue', value)
  if (!value) {
    reset()
  }
}

function reset() {
  step.value = 1
  loading.value = false
  selectedFile.value = null
  headers.value = []
  importMode.value = 'CREATE_ONLY'
  mapping.value = {}
  preview.value = null
  result.value = null
}

function normalizeHeader(header: string) {
  return header.toLowerCase().replace(/[^a-z0-9]/g, '')
}

function autoDetectMapping(headerList: string[]) {
  const knownMap: Record<string, Exclude<LeadImportTargetField, 'IGNORE'>> = {
    companyname: 'companyName',
    contactname: 'contactName',
    email: 'email',
    phone: 'phone',
    source: 'source',
    description: 'description',
    priority: 'priority',
    assigneduserid: 'assignedUserId',
    assignee: 'assignedUserId',
    templateid: 'templateId',
    template: 'templateId',
  }
  const nextMapping: LeadImportColumnMapping = {}
  headerList.forEach((header) => {
    nextMapping[header] = knownMap[normalizeHeader(header)] ?? 'IGNORE'
  })
  mapping.value = nextMapping
}

function parseCsvLine(line: string) {
  const values: string[] = []
  let current = ''
  let inQuotes = false
  for (let i = 0; i < line.length; i += 1) {
    const ch = line[i]
    if (ch === '"') {
      if (inQuotes && line[i + 1] === '"') {
        current += '"'
        i += 1
      } else {
        inQuotes = !inQuotes
      }
    } else if (ch === ',' && !inQuotes) {
      values.push(current)
      current = ''
    } else {
      current += ch
    }
  }
  values.push(current)
  return values.map((value) => value.trim())
}

async function handleFileSelection(files: File | File[] | null) {
  selectedFile.value = Array.isArray(files) ? files[0] ?? null : files
  preview.value = null
  result.value = null
  if (!selectedFile.value) {
    headers.value = []
    mapping.value = {}
    return
  }
  const text = await selectedFile.value.text()
  const [headerLine] = text.split(/\r?\n/)
  headers.value = headerLine ? parseCsvLine(headerLine) : []
  autoDetectMapping(headers.value)
  step.value = headers.value.length ? 3 : 2
}

async function downloadTemplate() {
  loading.value = true
  try {
    const blob = await importApi.downloadLeadImportTemplate()
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = 'lead-import-template.csv'
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)
    URL.revokeObjectURL(url)
    uiStore.showSuccess('Template downloaded')
  } finally {
    loading.value = false
  }
}

async function generatePreview() {
  if (!selectedFile.value || !canMoveToPreview.value) {
    return
  }
  loading.value = true
  try {
    preview.value = await importApi.previewLeadImport(selectedFile.value, {
      importMode: importMode.value,
      columnMappings: mapping.value,
    })
    step.value = 4
  } finally {
    loading.value = false
  }
}

async function runImport() {
  if (!selectedFile.value || !canRunImport.value) {
    return
  }
  loading.value = true
  try {
    result.value = await importApi.importLeads(selectedFile.value, {
      importMode: importMode.value,
      columnMappings: mapping.value,
    })
    step.value = 5
    uiStore.showSuccess(`Import finished: ${result.value.createdCount} created, ${result.value.updatedCount} updated`)
    emit('imported')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <v-dialog :model-value="modelValue" max-width="1180" scrollable @update:model-value="updateModelValue">
    <v-card class="glass-dialog-card import-dialog-card">
      <v-card-title class="d-flex align-center justify-space-between">
        <span>Import Leads</span>
        <v-btn icon="mdi-close" variant="text" @click="updateModelValue(false)" />
      </v-card-title>
      <v-divider />
      <v-card-text class="pa-6">
        <v-stepper v-model="step" alt-labels flat>
          <v-stepper-header>
            <v-stepper-item :value="1" title="Download template" />
            <v-divider />
            <v-stepper-item :value="2" title="Upload CSV" />
            <v-divider />
            <v-stepper-item :value="3" title="Map columns" />
            <v-divider />
            <v-stepper-item :value="4" title="Preview & validate" />
            <v-divider />
            <v-stepper-item :value="5" title="Run import" />
          </v-stepper-header>
        </v-stepper>

        <div class="mt-6 d-flex flex-column ga-6">
          <div v-if="step === 1" class="d-grid ga-4" style="grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);">
            <v-card variant="outlined" class="crm-card">
              <v-card-title>Start with the sample template</v-card-title>
              <v-card-text class="d-flex flex-column ga-4">
                <p class="text-body-1">
                  Download the CRM import template, fill in your rows, then come back here to upload and map your columns.
                </p>
                <div class="d-flex ga-3">
                  <v-btn color="primary" prepend-icon="mdi-download" :loading="loading" @click="downloadTemplate">Download template</v-btn>
                  <v-btn variant="tonal" color="secondary" @click="step = 2">Continue to upload</v-btn>
                </div>
              </v-card-text>
            </v-card>
            <LeadImportFieldGuide :fields="fieldGuide" />
          </div>

          <div v-else-if="step === 2" class="d-grid ga-4" style="grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);">
            <v-card variant="outlined" class="crm-card">
              <v-card-title>Upload a CSV file</v-card-title>
              <v-card-text class="d-flex flex-column ga-4">
                <v-file-input
                  accept=".csv,text/csv"
                  label="CSV file"
                  prepend-icon="mdi-file-delimited"
                  @update:model-value="handleFileSelection"
                />
                <div class="text-body-2 text-medium-emphasis">
                  We’ll read the header row, suggest column mappings, and validate rows before anything is imported.
                </div>
              </v-card-text>
            </v-card>
            <LeadImportFieldGuide :fields="fieldGuide" />
          </div>

          <div v-else-if="step === 3" class="d-flex flex-column ga-4">
            <div class="d-flex align-center justify-space-between flex-wrap ga-3">
              <div>
                <h3 class="text-h6">Map CSV columns</h3>
                <p class="text-body-2 text-medium-emphasis">Required fields are company name and contact name.</p>
              </div>
              <v-btn variant="tonal" color="secondary" @click="step = 2">Change file</v-btn>
            </div>
            <LeadImportMappingTable :headers="headers" :mapping="mapping" @update:mapping="mapping = $event" />
            <div class="d-flex justify-space-between align-center flex-wrap ga-3">
              <v-radio-group v-model="importMode" inline hide-details>
                <v-radio label="Create only" value="CREATE_ONLY" />
                <v-radio label="Upsert by email/phone" value="UPSERT_BY_EMAIL_OR_PHONE" />
              </v-radio-group>
              <v-btn color="primary" :disabled="!canMoveToPreview" :loading="loading" @click="generatePreview">
                Preview import
              </v-btn>
            </div>
          </div>

          <div v-else-if="step === 4 && preview" class="d-flex flex-column ga-4">
            <div class="d-flex align-center justify-space-between flex-wrap ga-3">
              <div>
                <h3 class="text-h6">Preview and validate</h3>
                <p class="text-body-2 text-medium-emphasis">Review warnings and duplicate hints before importing.</p>
              </div>
              <div class="d-flex ga-2">
                <v-btn variant="tonal" color="secondary" @click="step = 3">Back to mapping</v-btn>
                <v-btn color="primary" :disabled="!canRunImport" :loading="loading" @click="runImport">Run import</v-btn>
              </div>
            </div>
            <LeadImportPreviewTable :preview="preview" />
          </div>

          <div v-else-if="step === 5 && result" class="d-flex flex-column ga-4">
            <div>
              <h3 class="text-h6">Import complete</h3>
              <p class="text-body-2 text-medium-emphasis">Review the final result summary below.</p>
            </div>
            <v-row>
              <v-col cols="6" md="2">
                <v-sheet rounded class="pa-3 border workflow-kpi">
                  <div class="metric-label">Created</div>
                  <div class="text-h6">{{ result.createdCount }}</div>
                </v-sheet>
              </v-col>
              <v-col cols="6" md="2">
                <v-sheet rounded class="pa-3 border workflow-kpi">
                  <div class="metric-label">Updated</div>
                  <div class="text-h6">{{ result.updatedCount }}</div>
                </v-sheet>
              </v-col>
              <v-col cols="6" md="2">
                <v-sheet rounded class="pa-3 border workflow-kpi">
                  <div class="metric-label">Skipped</div>
                  <div class="text-h6">{{ result.skippedCount }}</div>
                </v-sheet>
              </v-col>
              <v-col cols="6" md="2">
                <v-sheet rounded class="pa-3 border workflow-kpi">
                  <div class="metric-label">Duplicates</div>
                  <div class="text-h6">{{ result.duplicateCount }}</div>
                </v-sheet>
              </v-col>
              <v-col cols="6" md="2">
                <v-sheet rounded class="pa-3 border workflow-kpi">
                  <div class="metric-label">Invalid</div>
                  <div class="text-h6">{{ result.invalidCount }}</div>
                </v-sheet>
              </v-col>
            </v-row>
            <v-table density="comfortable">
              <thead>
                <tr>
                  <th class="text-left">Row</th>
                  <th class="text-left">Outcome</th>
                  <th class="text-left">Messages</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in result.rowResults" :key="`${row.rowNumber}-${row.outcome}`">
                  <td>{{ row.rowNumber }}</td>
                  <td>{{ row.outcome }}</td>
                  <td>{{ row.messages.join(', ') || '—' }}</td>
                </tr>
              </tbody>
            </v-table>
          </div>
        </div>
      </v-card-text>
      <v-divider />
      <v-card-actions class="pa-4">
        <v-spacer />
        <v-btn variant="text" @click="updateModelValue(false)">Close</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.import-dialog-card {
  overflow: hidden;
}
</style>
