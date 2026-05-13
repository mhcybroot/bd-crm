<script setup lang="ts">
defineProps<{
  icon: string
  title: string
  text: string
}>()
</script>

<template>
  <v-card class="pa-8 text-center">
    <v-icon :icon="icon" size="48" color="primary" class="mb-4" />
    <div class="text-h6 mb-2">{{ title }}</div>
    <p class="text-medium-emphasis">{{ text }}</p>
  </v-card>
</template>
