<script setup lang="ts">
import type { LeadImportPreviewResponse } from '@/types/api'

defineProps<{
  preview: LeadImportPreviewResponse
}>()
</script>

<template>
  <div class="d-flex flex-column ga-4">
    <v-row>
      <v-col cols="6" md="3">
        <v-sheet rounded class="pa-3 border">
          <div class="text-caption text-medium-emphasis">Valid rows</div>
          <div class="text-h6">{{ preview.summary.validRows }}</div>
        </v-sheet>
      </v-col>
      <v-col cols="6" md="3">
        <v-sheet rounded class="pa-3 border">
          <div class="text-caption text-medium-emphasis">Warnings</div>
          <div class="text-h6">{{ preview.summary.warningRows }}</div>
        </v-sheet>
      </v-col>
      <v-col cols="6" md="3">
        <v-sheet rounded class="pa-3 border">
          <div class="text-caption text-medium-emphasis">Invalid rows</div>
          <div class="text-h6">{{ preview.summary.invalidRows }}</div>
        </v-sheet>
      </v-col>
      <v-col cols="6" md="3">
        <v-sheet rounded class="pa-3 border">
          <div class="text-caption text-medium-emphasis">Possible duplicates</div>
          <div class="text-h6">{{ preview.summary.duplicateRows }}</div>
        </v-sheet>
      </v-col>
    </v-row>

    <v-alert
      v-for="warning in preview.warnings"
      :key="warning"
      type="info"
      variant="tonal"
      density="comfortable"
    >
      {{ warning }}
    </v-alert>

    <v-table density="comfortable">
      <thead>
        <tr>
          <th class="text-left">Row</th>
          <th class="text-left">Mapped values</th>
          <th class="text-left">Status</th>
          <th class="text-left">Action</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in preview.rows" :key="row.rowNumber">
          <td>{{ row.rowNumber }}</td>
          <td>
            <div class="d-flex flex-wrap ga-2">
              <v-chip
                v-for="(value, key) in row.values"
                :key="key"
                size="small"
                variant="tonal"
              >
                {{ key }}: {{ value || '—' }}
              </v-chip>
            </div>
          </td>
          <td>
            <div class="d-flex flex-column ga-2">
              <v-chip
                size="small"
                :color="row.issues.length ? 'error' : row.warnings.length ? 'warning' : 'success'"
                variant="tonal"
              >
                {{ row.issues.length ? 'Invalid' : row.warnings.length ? 'Warning' : 'Valid' }}
              </v-chip>
              <div v-if="row.duplicateSuspected" class="text-caption text-warning">Duplicate suspected</div>
              <div v-for="issue in row.issues" :key="`${row.rowNumber}-${issue.field}-${issue.message}`" class="text-caption text-error">
                {{ issue.field }}: {{ issue.message }}
              </div>
              <div v-for="warning in row.warnings" :key="`${row.rowNumber}-${warning}`" class="text-caption text-warning">
                {{ warning }}
              </div>
            </div>
          </td>
          <td>{{ row.suggestedAction }}</td>
        </tr>
      </tbody>
    </v-table>
  </div>
</template>
