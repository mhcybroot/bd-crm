<template>
  <RouterView v-slot="{ Component, route }">
    <transition name="crm-route" mode="out-in">
      <component :is="Component" :key="route.fullPath" />
    </transition>
  </RouterView>
</template>
