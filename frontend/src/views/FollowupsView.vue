<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import * as followupApi from '@/api/followups'
import * as userApi from '@/api/users'
import StatusChip from '@/components/common/StatusChip.vue'
import { useUiStore } from '@/stores/ui'
import type { FollowupActionRequest, FollowupOutcome, LeadFollowupResponse, UserResponse } from '@/types/api'
import { formatDate } from '@/utils/formatters'

const uiStore = useUiStore()
const statusFilter = ref('open')
const loading = ref(true)
const followups = ref<LeadFollowupResponse[]>([])
const users = ref<UserResponse[]>([])
const dialog = reactive({
  open: false,
  mode: 'complete' as 'complete' | 'reschedule' | 'skip' | 'reassign',
  followupId: 0,
  dueDate: '',
  outcome: null as FollowupOutcome | null,
  assignedUserId: null as number | null,
  notes: '',
})

async function load() {
  loading.value = true
  try {
    followups.value = await followupApi.listFollowups(statusFilter.value)
    users.value = await userApi.listUsers().catch(() => [])
  } finally {
    loading.value = false
  }
}

watch(statusFilter, load)
onMounted(load)

function openDialog(mode: typeof dialog.mode, followupId: number) {
  Object.assign(dialog, { open: true, mode, followupId, dueDate: '', outcome: null, assignedUserId: null, notes: '' })
}

async function submitAction() {
  if (dialog.mode === 'complete' && !dialog.outcome) {
    uiStore.showError('Outcome is required before completing a follow-up')
    return
  }
  const payload: FollowupActionRequest = {
    dueDate: dialog.dueDate || null,
    outcome: dialog.outcome,
    assignedUserId: dialog.assignedUserId,
    notes: dialog.notes,
  }
  try {
    if (dialog.mode === 'complete') await followupApi.completeFollowup(dialog.followupId, payload)
    if (dialog.mode === 'reschedule') await followupApi.rescheduleFollowup(dialog.followupId, payload)
    if (dialog.mode === 'skip') await followupApi.skipFollowup(dialog.followupId, payload)
    if (dialog.mode === 'reassign') await followupApi.reassignFollowup(dialog.followupId, payload)
    dialog.open = false
    uiStore.showSuccess('Follow-up updated')
    await load()
  } catch (error) {
    uiStore.showError(error instanceof Error ? error.message : 'Unable to update follow-up')
  }
}
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">Follow-up Queue</h1>
        <p class="page-subtitle">Work the open cadence, handle overdue items, and keep ownership clear.</p>
      </div>
      <v-select v-model="statusFilter" label="Queue" :items="['open', 'due', 'overdue', 'completed']" max-width="180" />
    </div>

    <v-card>
      <v-data-table :items="followups" :loading="loading">
        <template #headers>
          <tr>
            <th class="text-left">Step</th>
            <th class="text-left">Due</th>
            <th class="text-left">Owner</th>
            <th class="text-left">Status</th>
            <th class="text-left">Channel</th>
            <th class="text-left">Actions</th>
          </tr>
        </template>
        <template #item="{ item }">
          <tr>
            <td>{{ item.stepNumber }}</td>
            <td>{{ formatDate(item.dueDate) }}</td>
            <td>{{ item.assignedUserName }}</td>
            <td><StatusChip :value="item.status" /></td>
            <td>{{ item.channel }}</td>
            <td>
              <div class="d-flex ga-2">
                <v-btn size="small" color="success" variant="tonal" @click="openDialog('complete', item.id)">Complete</v-btn>
                <v-btn size="small" color="warning" variant="tonal" @click="openDialog('reschedule', item.id)">Reschedule</v-btn>
                <v-btn size="small" color="secondary" variant="tonal" @click="openDialog('reassign', item.id)">Reassign</v-btn>
                <v-btn size="small" color="error" variant="tonal" @click="openDialog('skip', item.id)">Skip</v-btn>
              </div>
            </td>
          </tr>
        </template>
      </v-data-table>
    </v-card>

    <v-dialog v-model="dialog.open" max-width="520">
      <v-card>
        <v-card-title class="text-capitalize">{{ dialog.mode }} follow-up</v-card-title>
        <v-card-text>
          <v-select
            v-if="dialog.mode === 'complete'"
            v-model="dialog.outcome"
            label="Outcome"
            :items="['NO_RESPONSE', 'INTERESTED', 'NOT_INTERESTED', 'CALLBACK_REQUESTED', 'QUALIFIED', 'WON', 'LOST']"
          />
          <v-text-field v-if="dialog.mode === 'reschedule'" v-model="dialog.dueDate" type="date" label="New due date" />
          <v-select
            v-if="dialog.mode === 'reassign'"
            v-model="dialog.assignedUserId"
            label="Assign to"
            :items="users"
            item-title="fullName"
            item-value="id"
          />
          <v-textarea v-model="dialog.notes" label="Notes" rows="4" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialog.open = false">Cancel</v-btn>
          <v-btn color="primary" @click="submitAction">Save</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>
