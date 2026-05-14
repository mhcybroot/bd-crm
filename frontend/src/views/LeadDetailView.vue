<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as leadApi from '@/api/leads'
import * as attachmentApi from '@/api/attachments'
import * as templateApi from '@/api/templates'
import * as userApi from '@/api/users'
import FollowupTimeline from '@/components/followups/FollowupTimeline.vue'
import StatusChip from '@/components/common/StatusChip.vue'
import { useUiStore } from '@/stores/ui'
import type { ContactChannel, FollowupTemplateResponse, LeadCommunicationRequest, LeadDetailResponse, LeadQualificationRequest, LeadStatus, UserResponse } from '@/types/api'
import { formatDateTime } from '@/utils/formatters'

const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()
const loading = ref(true)
const lead = ref<LeadDetailResponse | null>(null)
const loadError = ref('')
const users = ref<UserResponse[]>([])
const templates = ref<FollowupTemplateResponse[]>([])
const noteBody = ref('')
const uploadFile = ref<File | null>(null)
const actions = reactive({
  status: 'NEW' as LeadStatus,
  assignedUserId: null as number | null,
  templateId: null as number | null,
  stageId: null as number | null,
})
const qualification = reactive<LeadQualificationRequest>({
  budgetRange: '',
  authorityLevel: '',
  needSummary: '',
  timelineTarget: '',
  fitScore: 0,
  engagementScore: 0,
  qualificationNotes: '',
})
const communication = reactive<LeadCommunicationRequest>({
  channel: 'CALL' as ContactChannel,
  subject: '',
  body: '',
  outcome: '',
  occurredAt: null,
})
const currentTemplateStages = computed(() =>
  templates.value.find((template) => template.id === lead.value?.lead.templateId)?.stages ?? [],
)

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const [leadData, userData, templateData] = await Promise.all([
      leadApi.getLead(Number(route.params.id)),
      userApi.listUsers().catch(() => []),
      templateApi.listTemplates(),
    ])
    lead.value = leadData
    users.value = userData
    templates.value = templateData
    actions.status = leadData.lead.status
    actions.assignedUserId = leadData.lead.assignedUserId
    actions.templateId = leadData.lead.templateId
    actions.stageId = leadData.lead.currentStageId
    Object.assign(qualification, {
      budgetRange: leadData.qualification.budgetRange || '',
      authorityLevel: leadData.qualification.authorityLevel || '',
      needSummary: leadData.qualification.needSummary || '',
      timelineTarget: leadData.qualification.timelineTarget || '',
      fitScore: leadData.qualification.fitScore,
      engagementScore: leadData.qualification.engagementScore,
      qualificationNotes: leadData.qualification.qualificationNotes || '',
    })
  } catch (error) {
    lead.value = null
    loadError.value = error instanceof Error ? error.message : 'Unable to load lead detail'
  } finally {
    loading.value = false
  }
}

async function addNote() {
  if (!noteBody.value.trim()) return
  try {
    await leadApi.addLeadNote(Number(route.params.id), { body: noteBody.value })
    noteBody.value = ''
    uiStore.showSuccess('Note added')
    await load()
  } catch (error) {
    uiStore.showError(error instanceof Error ? error.message : 'Unable to add note')
  }
}

async function updateStatus() {
  try {
    await leadApi.updateLeadStatus(Number(route.params.id), { status: actions.status })
    uiStore.showSuccess('Lead status updated')
    await load()
  } catch (error) {
    uiStore.showError(error instanceof Error ? error.message : 'Unable to update status')
  }
}

async function updateAssignment() {
  if (!actions.assignedUserId) return
  try {
    await leadApi.assignLead(Number(route.params.id), { assignedUserId: actions.assignedUserId })
    uiStore.showSuccess('Lead owner updated')
    await load()
  } catch (error) {
    uiStore.showError(error instanceof Error ? error.message : 'Unable to update owner')
  }
}

async function updateStage() {
  if (!actions.stageId) return
  try {
    await leadApi.updateLeadStage(Number(route.params.id), { stageId: actions.stageId, note: 'Updated from lead detail' })
    uiStore.showSuccess('Lead stage updated')
    await load()
  } catch (error) {
    uiStore.showError(error instanceof Error ? error.message : 'Unable to update stage')
  }
}

async function saveQualification() {
  try {
    await leadApi.updateQualification(Number(route.params.id), qualification)
    uiStore.showSuccess('Qualification updated')
    await load()
  } catch (error) {
    uiStore.showError(error instanceof Error ? error.message : 'Unable to update qualification')
  }
}

async function addCommunication() {
  try {
    await leadApi.addCommunication(Number(route.params.id), communication)
    Object.assign(communication, { channel: 'CALL', subject: '', body: '', outcome: '', occurredAt: null })
    uiStore.showSuccess('Communication logged')
    await load()
  } catch (error) {
    uiStore.showError(error instanceof Error ? error.message : 'Unable to log communication')
  }
}

async function uploadAttachment() {
  if (!uploadFile.value) return
  try {
    await attachmentApi.uploadAttachment(Number(route.params.id), uploadFile.value)
    uploadFile.value = null
    uiStore.showSuccess('Attachment uploaded')
    await load()
  } catch (error) {
    uiStore.showError(error instanceof Error ? error.message : 'Unable to upload attachment')
  }
}

function selectUploadFile(files: File | File[] | null) {
  uploadFile.value = Array.isArray(files) ? files[0] ?? null : files
}

onMounted(load)
</script>

<template>
  <div v-if="loading" class="pa-6 text-medium-emphasis page-shell">Loading lead...</div>
  <v-alert v-else-if="loadError" type="error" variant="tonal" class="mb-4">
    {{ loadError }}
  </v-alert>
  <div v-if="lead" class="page-shell">
    <div class="page-header page-hero">
      <div>
        <h1 class="page-title">{{ lead.lead.companyName }}</h1>
        <p class="page-subtitle">{{ lead.lead.contactName }} · {{ lead.lead.email || 'No email' }}</p>
      </div>
      <div class="crm-hero-actions">
        <v-chip color="primary" variant="flat">{{ lead.lead.currentStageName || 'Unstaged' }}</v-chip>
        <v-btn variant="outlined" @click="router.push(`/leads/${lead.lead.id}/edit`)">Edit</v-btn>
      </div>
    </div>

    <v-row>
      <v-col cols="12" md="4">
        <v-card class="mb-4 crm-card">
          <v-card-title>Lead Snapshot</v-card-title>
          <v-card-text class="d-flex flex-column ga-3">
            <div class="d-flex justify-space-between"><span>Status</span><StatusChip :value="lead.lead.status" /></div>
            <div class="d-flex justify-space-between"><span>Priority</span><StatusChip :value="lead.lead.priority" /></div>
            <div class="d-flex justify-space-between"><span>Owner</span><span>{{ lead.lead.assignedUserName }}</span></div>
            <div class="d-flex justify-space-between"><span>Template</span><span>{{ lead.lead.templateName }}</span></div>
            <div class="d-flex justify-space-between"><span>Stage</span><span>{{ lead.lead.currentStageName || '-' }}</span></div>
            <div class="d-flex justify-space-between"><span>Duplicate</span><span>{{ lead.lead.duplicateState }}</span></div>
            <div class="d-flex justify-space-between"><span>Phone</span><span>{{ lead.lead.phone || '-' }}</span></div>
            <div class="d-flex justify-space-between"><span>Source</span><span>{{ lead.lead.source || '-' }}</span></div>
            <div class="d-flex justify-space-between"><span>Score</span><span>{{ lead.score.totalScore }}/100</span></div>
          </v-card-text>
        </v-card>

        <v-card class="mb-4 crm-card">
          <v-card-title>Quick Actions</v-card-title>
          <v-card-text>
            <v-select v-model="actions.status" label="Update status" :items="['NEW', 'IN_PROGRESS', 'QUALIFIED', 'WON', 'LOST', 'DORMANT']" />
            <v-btn block color="primary" class="mb-4" @click="updateStatus">Save status</v-btn>
            <v-select
              v-model="actions.assignedUserId"
              label="Reassign owner"
              :items="users"
              item-title="fullName"
              item-value="id"
            />
            <v-btn block color="secondary" class="mb-4" @click="updateAssignment">Save owner</v-btn>
            <v-select
              v-model="actions.stageId"
              label="Pipeline stage"
              :items="currentTemplateStages"
              item-title="name"
              item-value="id"
            />
            <v-btn block color="info" @click="updateStage">Save stage</v-btn>
          </v-card-text>
        </v-card>

        <v-card class="mb-4 crm-card">
          <v-card-title>Add Note</v-card-title>
          <v-card-text>
            <v-textarea v-model="noteBody" label="Conversation details or next step" rows="4" />
            <v-btn color="primary" block @click="addNote">Add note</v-btn>
          </v-card-text>
        </v-card>

        <v-card class="mb-4 crm-card">
          <v-card-title>Qualification</v-card-title>
          <v-card-text>
            <v-text-field v-model="qualification.budgetRange" label="Budget range" />
            <v-text-field v-model="qualification.authorityLevel" label="Authority level" />
            <v-text-field v-model="qualification.timelineTarget" label="Timeline target" />
            <v-textarea v-model="qualification.needSummary" label="Need summary" rows="3" />
            <v-text-field v-model.number="qualification.fitScore" type="number" label="Fit score" />
            <v-text-field v-model.number="qualification.engagementScore" type="number" label="Engagement score" />
            <v-textarea v-model="qualification.qualificationNotes" label="Qualification notes" rows="3" />
            <v-btn color="primary" block @click="saveQualification">Save qualification</v-btn>
          </v-card-text>
        </v-card>

        <v-card class="crm-card">
          <v-card-title>Attachments</v-card-title>
          <v-card-text>
            <v-file-input label="Upload file" @update:model-value="selectUploadFile" />
            <v-btn color="primary" block class="mb-4" @click="uploadAttachment">Upload attachment</v-btn>
            <v-list density="compact">
              <v-list-item v-for="attachment in lead.attachments" :key="attachment.id" :title="attachment.originalFileName" :subtitle="attachment.uploadedByUserName" />
            </v-list>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="8">
        <v-card class="mb-4 crm-card">
          <v-card-title>7-Step Follow-up Tracker</v-card-title>
          <v-card-text>
            <FollowupTimeline :followups="lead.followups" />
          </v-card-text>
        </v-card>

        <v-card class="mb-4 crm-card">
          <v-card-title>Communications</v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="12" md="4"><v-select v-model="communication.channel" label="Channel" :items="['CALL', 'EMAIL', 'WHATSAPP', 'LINKEDIN', 'MEETING']" /></v-col>
              <v-col cols="12" md="4"><v-text-field v-model="communication.subject" label="Subject" /></v-col>
              <v-col cols="12" md="4"><v-text-field v-model="communication.outcome" label="Outcome" /></v-col>
              <v-col cols="12"><v-textarea v-model="communication.body" label="Details" rows="3" /></v-col>
            </v-row>
            <v-btn color="primary" @click="addCommunication">Log communication</v-btn>
            <v-list class="mt-4" lines="three">
              <v-list-item v-for="entry in lead.communications" :key="entry.id">
                <v-list-item-title>{{ entry.channel }} · {{ entry.actorName }}</v-list-item-title>
                <v-list-item-subtitle>{{ formatDateTime(entry.occurredAt) }}</v-list-item-subtitle>
                <template #append>
                  <div class="text-body-2">{{ entry.subject || entry.body }}</div>
                </template>
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>

        <v-card class="mb-4 crm-card">
          <v-card-title>Notes</v-card-title>
          <v-list lines="three">
            <v-list-item v-for="note in lead.notes" :key="note.id">
              <v-list-item-title>{{ note.authorName }}</v-list-item-title>
              <v-list-item-subtitle>{{ formatDateTime(note.createdAt) }}</v-list-item-subtitle>
              <template #append>
                <div class="text-body-2">{{ note.body }}</div>
              </template>
            </v-list-item>
          </v-list>
        </v-card>

        <v-card class="crm-card">
          <v-card-title>Activity and Stage Timeline</v-card-title>
          <v-timeline density="compact" side="end">
            <v-timeline-item v-for="history in lead.stageHistory" :key="`stage-${history.id}`" dot-color="primary" size="small">
              <v-card class="pa-4 mb-4">
                <div class="text-subtitle-2">Entered {{ history.stageName }}</div>
                <div class="text-body-2 text-medium-emphasis">
                  {{ history.changedByUserName }} · {{ formatDateTime(history.enteredAt) }}
                </div>
              </v-card>
            </v-timeline-item>
            <v-timeline-item v-for="activity in lead.activities" :key="activity.id" dot-color="secondary" size="small">
              <v-card class="pa-4">
                <div class="text-subtitle-2">{{ activity.description }}</div>
                <div class="text-body-2 text-medium-emphasis">
                  {{ activity.actorName }} · {{ formatDateTime(activity.createdAt) }}
                </div>
              </v-card>
            </v-timeline-item>
          </v-timeline>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>
