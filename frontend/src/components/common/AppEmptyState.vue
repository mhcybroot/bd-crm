<script setup lang="ts">
defineProps<{
  icon: string
  title: string
  text: string
}>()
</script>

<template>
  <v-card class="pa-8 text-center crm-card empty-state-card">
    <div class="empty-state-orb mx-auto mb-4">
      <v-icon :icon="icon" size="42" color="primary" />
    </div>
    <div class="section-heading mb-2">{{ title }}</div>
    <p class="text-medium-emphasis empty-state-copy">{{ text }}</p>
  </v-card>
</template>

<style scoped>
.empty-state-card {
  background:
    linear-gradient(180deg, rgba(255, 253, 248, 0.94), rgba(255, 247, 240, 0.84)),
    radial-gradient(circle at top, rgba(15, 118, 110, 0.14), transparent 38%);
}

.empty-state-orb {
  display: grid;
  place-items: center;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(15, 118, 110, 0.16), rgba(234, 88, 12, 0.14));
  box-shadow: 0 18px 36px rgba(15, 118, 110, 0.15);
}

.empty-state-copy {
  max-width: 34ch;
  margin: 0 auto;
}
</style>
