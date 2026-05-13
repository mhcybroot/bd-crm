<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as leadApi from '@/api/leads'
import * as templateApi from '@/api/templates'
import * as userApi from '@/api/users'
import FollowupTimeline from '@/components/followups/FollowupTimeline.vue'
import StatusChip from '@/components/common/StatusChip.vue'
import { useUiStore } from '@/stores/ui'
import type { FollowupTemplateResponse, LeadDetailResponse, LeadStatus, UserResponse } from '@/types/api'
import { formatDateTime } from '@/utils/formatters'

const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()
const loading = ref(true)
const lead = ref<LeadDetailResponse | null>(null)
const users = ref<UserResponse[]>([])
const templates = ref<FollowupTemplateResponse[]>([])
const noteBody = ref('')
const actions = reactive({
  status: 'NEW' as LeadStatus,
  assignedUserId: null as number | null,
  templateId: null as number | null,
})

async function load() {
  loading.value = true
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

onMounted(load)
</script>

<template>
  <div v-if="lead">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ lead.lead.companyName }}</h1>
        <p class="page-subtitle">{{ lead.lead.contactName }} · {{ lead.lead.email || 'No email' }}</p>
      </div>
      <div class="d-flex ga-3">
        <v-btn variant="outlined" @click="router.push(`/leads/${lead.lead.id}/edit`)">Edit</v-btn>
      </div>
    </div>

    <v-row>
      <v-col cols="12" md="4">
        <v-card class="mb-4">
          <v-card-title>Lead Snapshot</v-card-title>
          <v-card-text class="d-flex flex-column ga-3">
            <div class="d-flex justify-space-between"><span>Status</span><StatusChip :value="lead.lead.status" /></div>
            <div class="d-flex justify-space-between"><span>Priority</span><StatusChip :value="lead.lead.priority" /></div>
            <div class="d-flex justify-space-between"><span>Owner</span><span>{{ lead.lead.assignedUserName }}</span></div>
            <div class="d-flex justify-space-between"><span>Template</span><span>{{ lead.lead.templateName }}</span></div>
            <div class="d-flex justify-space-between"><span>Phone</span><span>{{ lead.lead.phone || '-' }}</span></div>
            <div class="d-flex justify-space-between"><span>Source</span><span>{{ lead.lead.source || '-' }}</span></div>
          </v-card-text>
        </v-card>

        <v-card class="mb-4">
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
            <v-btn block color="secondary" @click="updateAssignment">Save owner</v-btn>
          </v-card-text>
        </v-card>

        <v-card>
          <v-card-title>Add Note</v-card-title>
          <v-card-text>
            <v-textarea v-model="noteBody" label="Conversation details or next step" rows="4" />
            <v-btn color="primary" block @click="addNote">Add note</v-btn>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="8">
        <v-card class="mb-4">
          <v-card-title>7-Step Follow-up Tracker</v-card-title>
          <v-card-text>
            <FollowupTimeline :followups="lead.followups" />
          </v-card-text>
        </v-card>

        <v-card class="mb-4">
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

        <v-card>
          <v-card-title>Activity Timeline</v-card-title>
          <v-timeline density="compact" side="end">
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
