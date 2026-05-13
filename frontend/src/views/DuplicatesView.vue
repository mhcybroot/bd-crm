<script setup lang="ts">
import { onMounted, ref } from 'vue'
import * as duplicateApi from '@/api/duplicates'
import { useUiStore } from '@/stores/ui'
import type { DuplicateCandidateResponse } from '@/types/api'

const uiStore = useUiStore()
const duplicates = ref<DuplicateCandidateResponse[]>([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    duplicates.value = await duplicateApi.listDuplicates()
  } finally {
    loading.value = false
  }
}

async function rescan() {
  duplicates.value = await duplicateApi.scanDuplicates()
  uiStore.showSuccess('Duplicate scan completed')
}

async function updateState(id: number, state: 'REVIEWED' | 'CLEAR') {
  await duplicateApi.updateDuplicateState(id, state)
  await load()
}

async function merge(item: DuplicateCandidateResponse) {
  await duplicateApi.mergeLeads({
    sourceLeadId: item.matchedLeadId,
    targetLeadId: item.leadId,
    summary: `Merged ${item.matchedLeadCompanyName} into ${item.leadCompanyName}`,
  })
  uiStore.showSuccess('Leads merged')
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">Duplicate Review</h1>
        <p class="page-subtitle">Catch suspected lead collisions before they distort the pipeline and reports.</p>
      </div>
      <v-btn color="primary" @click="rescan">Run scan</v-btn>
    </div>

    <v-card>
      <v-data-table :items="duplicates" :loading="loading">
        <template #headers>
          <tr>
            <th class="text-left">Lead</th>
            <th class="text-left">Matched Lead</th>
            <th class="text-left">Score</th>
            <th class="text-left">Reason</th>
            <th class="text-left">State</th>
            <th class="text-left">Actions</th>
          </tr>
        </template>
        <template #item="{ item }">
          <tr>
            <td>{{ item.leadCompanyName }}</td>
            <td>{{ item.matchedLeadCompanyName }}</td>
            <td>{{ item.matchScore }}</td>
            <td>{{ item.reason }}</td>
            <td>{{ item.state }}</td>
            <td>
              <div class="d-flex ga-2">
                <v-btn size="small" variant="tonal" color="primary" @click="updateState(item.id, 'REVIEWED')">Review</v-btn>
                <v-btn size="small" variant="tonal" color="success" @click="updateState(item.id, 'CLEAR')">Ignore</v-btn>
                <v-btn size="small" variant="tonal" color="warning" @click="merge(item)">Merge</v-btn>
              </div>
            </td>
          </tr>
        </template>
      </v-data-table>
    </v-card>
  </div>
</template>
