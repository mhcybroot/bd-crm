<script setup lang="ts">
import { computed } from 'vue'
import { titleCase } from '@/utils/formatters'

const props = defineProps<{
  value: string
}>()

const color = computed(() => {
  if (['WON', 'COMPLETED', 'QUALIFIED', 'ACTIVE'].includes(props.value)) return 'success'
  if (['OVERDUE', 'LOST', 'CANCELLED'].includes(props.value)) return 'error'
  if (['SUSPENDED', 'BLOCKED'].includes(props.value)) return 'warning'
  if (['ARCHIVED'].includes(props.value)) return 'secondary'
  if (['DUE', 'IN_PROGRESS', 'HIGH', 'ESCALATED'].includes(props.value)) return 'warning'
  return 'primary'
})
</script>

<template>
  <v-chip :color="color" size="small" variant="tonal" class="status-chip">
    {{ titleCase(value) }}
  </v-chip>
</template>

<style scoped>
.status-chip {
  min-width: 88px;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.45);
  font-weight: 800;
  letter-spacing: 0.03em;
  backdrop-filter: blur(12px);
}
</style>
