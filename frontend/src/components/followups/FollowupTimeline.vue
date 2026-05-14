<script setup lang="ts">
import StatusChip from '@/components/common/StatusChip.vue'
import type { LeadFollowupResponse } from '@/types/api'
import { formatDate, formatDateTime, titleCase } from '@/utils/formatters'

defineProps<{
  followups: LeadFollowupResponse[]
}>()
</script>

<template>
  <v-timeline density="compact" side="end">
    <v-timeline-item
      v-for="followup in followups"
      :key="followup.id"
      dot-color="primary"
      fill-dot
      size="small"
    >
      <v-card class="pa-4 crm-card followup-timeline-card">
        <div class="d-flex justify-space-between flex-wrap ga-3 mb-2">
          <div>
            <div class="section-heading">Follow-up {{ followup.stepNumber }}</div>
            <div class="text-body-2 text-medium-emphasis">
              {{ titleCase(followup.channel) }} · Due {{ formatDate(followup.dueDate) }}
            </div>
          </div>
          <div class="d-flex ga-2 align-center">
            <StatusChip :value="followup.status" />
            <v-chip v-if="followup.escalatedAt" color="error" size="small" variant="tonal">
              Escalated
            </v-chip>
          </div>
        </div>
        <div class="text-body-2 mb-2">{{ followup.instructions || 'No instructions provided.' }}</div>
        <div class="text-body-2 text-medium-emphasis">
          Owner: {{ followup.assignedUserName }}<span v-if="followup.completedAt"> · Completed {{ formatDateTime(followup.completedAt) }}</span>
        </div>
        <div v-if="followup.notes" class="mt-2 text-body-2">Notes: {{ followup.notes }}</div>
      </v-card>
    </v-timeline-item>
  </v-timeline>
</template>

<style scoped>
.followup-timeline-card {
  background:
    linear-gradient(180deg, rgba(255, 253, 248, 0.94), rgba(255, 247, 240, 0.84)),
    radial-gradient(circle at top right, rgba(15, 118, 110, 0.1), transparent 36%);
}
</style>
