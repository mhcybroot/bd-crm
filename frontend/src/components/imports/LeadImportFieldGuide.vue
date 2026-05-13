<script setup lang="ts">
import type { LeadImportTemplateField } from '@/types/api'

defineProps<{
  fields: LeadImportTemplateField[]
}>()
</script>

<template>
  <v-card variant="tonal" color="surface">
    <v-card-title class="text-subtitle-1">Field Guide</v-card-title>
    <v-card-text class="d-flex flex-column ga-3">
      <div
        v-for="field in fields"
        :key="field.key"
        class="guide-row"
      >
        <div class="d-flex align-center ga-2 mb-1">
          <strong>{{ field.label }}</strong>
          <v-chip size="x-small" :color="field.required ? 'primary' : 'default'" variant="tonal">
            {{ field.required ? 'Required' : 'Optional' }}
          </v-chip>
        </div>
        <div class="text-body-2 text-medium-emphasis">Format: {{ field.formatHint }}</div>
        <div class="text-body-2 text-medium-emphasis">Example: {{ field.example }}</div>
      </div>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.guide-row + .guide-row {
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  padding-top: 12px;
}
</style>
