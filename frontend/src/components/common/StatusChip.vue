<script setup lang="ts">
import { computed } from 'vue'
import { titleCase } from '@/utils/formatters'

const props = defineProps<{
  value: string
}>()

const color = computed(() => {
  if (['WON', 'COMPLETED', 'QUALIFIED', 'ACTIVE'].includes(props.value)) return 'success'
  if (['OVERDUE', 'LOST', 'CANCELLED'].includes(props.value)) return 'error'
  if (['DUE', 'IN_PROGRESS', 'HIGH', 'ESCALATED'].includes(props.value)) return 'warning'
  return 'primary'
})
</script>

<template>
  <v-chip :color="color" size="small" variant="tonal">
    {{ titleCase(value) }}
  </v-chip>
</template>
