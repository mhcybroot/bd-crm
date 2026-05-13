<script setup lang="ts">
import type { LeadImportColumnMapping, LeadImportTargetField } from '@/types/api'

const props = defineProps<{
  headers: string[]
  mapping: LeadImportColumnMapping
}>()

const emit = defineEmits<{
  (e: 'update:mapping', value: LeadImportColumnMapping): void
}>()

const mappingOptions: Array<{ title: string; value: LeadImportTargetField }> = [
  { title: 'Company name', value: 'companyName' },
  { title: 'Contact name', value: 'contactName' },
  { title: 'Email', value: 'email' },
  { title: 'Phone', value: 'phone' },
  { title: 'Source', value: 'source' },
  { title: 'Description', value: 'description' },
  { title: 'Priority', value: 'priority' },
  { title: 'Assignee', value: 'assignedUserId' },
  { title: 'Follow-up template', value: 'templateId' },
  { title: 'Ignore column', value: 'IGNORE' },
]

function update(header: string, value: LeadImportTargetField) {
  emit('update:mapping', {
    ...props.mapping,
    [header]: value,
  })
}
</script>

<template>
  <v-table density="comfortable">
    <thead>
      <tr>
        <th class="text-left">CSV Header</th>
        <th class="text-left">Map To</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="header in headers" :key="header">
        <td>{{ header }}</td>
        <td style="min-width: 260px">
          <v-select
            :model-value="mapping[header] ?? 'IGNORE'"
            :items="mappingOptions"
            item-title="title"
            item-value="value"
            density="comfortable"
            hide-details
            @update:model-value="update(header, $event)"
          />
        </td>
      </tr>
    </tbody>
  </v-table>
</template>
