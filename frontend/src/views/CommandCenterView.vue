<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as dashboardApi from '@/api/dashboard'
import type { CommandCenterResponse } from '@/types/api'

const router = useRouter()
const loading = ref(true)
const data = ref<CommandCenterResponse | null>(null)

async function load() {
  loading.value = true
  try {
    data.value = await dashboardApi.getCommandCenter()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page-shell">
    <div class="page-header page-hero">
      <div>
        <h1 class="page-title">Command Center</h1>
        <p class="page-subtitle">Work the team’s priority alerts, duplicates, and recommended next actions from one place.</p>
      </div>
      <div class="crm-hero-actions">
        <v-chip color="primary" variant="flat">{{ data?.recommendations.length ?? 0 }} recommendations</v-chip>
        <v-chip color="warning" variant="tonal">{{ data?.duplicates.length ?? 0 }} duplicate alerts</v-chip>
      </div>
    </div>

    <v-row class="crm-stagger">
      <v-col cols="12" md="4">
        <v-card class="pa-4 h-100 crm-card">
          <div class="section-kicker">Prioritize</div>
          <div class="section-heading mb-3">Recommendations</div>
          <v-list density="compact">
            <v-list-item v-for="recommendation in data?.recommendations ?? []" :key="recommendation" :title="recommendation" />
            <v-list-item v-if="!(data?.recommendations?.length)" title="No recommendations right now" />
          </v-list>
        </v-card>
      </v-col>
      <v-col cols="12" md="4">
        <v-card class="pa-4 h-100 crm-card">
          <div class="section-kicker">Execution</div>
          <div class="section-heading mb-3">Due Follow-ups</div>
          <v-list density="compact">
            <v-list-item
              v-for="followup in data?.dueFollowups ?? []"
              :key="followup.id"
              :title="`${followup.assignedUserName} · Follow-up ${followup.stepNumber}`"
              :subtitle="followup.dueDate"
              @click="router.push('/followups')"
            />
          </v-list>
        </v-card>
      </v-col>
      <v-col cols="12" md="4">
        <v-card class="pa-4 h-100 crm-card">
          <div class="section-kicker">Data Health</div>
          <div class="section-heading mb-3">Duplicates</div>
          <v-list density="compact">
            <v-list-item
              v-for="duplicate in data?.duplicates ?? []"
              :key="duplicate.id"
              :title="`${duplicate.leadCompanyName} vs ${duplicate.matchedLeadCompanyName}`"
              :subtitle="`Score ${duplicate.matchScore}`"
              @click="router.push('/duplicates')"
            />
            <v-list-item v-if="!(data?.duplicates?.length)" title="No duplicate review queue" />
          </v-list>
        </v-card>
      </v-col>
    </v-row>

    <v-card class="mt-4 crm-card">
      <v-card-title>Latest Notifications</v-card-title>
      <v-data-table :items="data?.notifications ?? []" :loading="loading">
        <template #headers>
          <tr>
            <th class="text-left">Type</th>
            <th class="text-left">Title</th>
            <th class="text-left">Message</th>
            <th class="text-left">Created</th>
          </tr>
        </template>
        <template #item="{ item }">
          <tr class="crm-table-row">
            <td>{{ item.type }}</td>
            <td>{{ item.title }}</td>
            <td>{{ item.message }}</td>
            <td>{{ item.createdAt }}</td>
          </tr>
        </template>
      </v-data-table>
    </v-card>
  </div>
</template>
