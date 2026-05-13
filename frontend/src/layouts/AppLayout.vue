<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import type { RoleName } from '@/types/api'

const drawer = ref(true)
const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUiStore()

type NavItem = {
  title: string
  to: string
  icon: string
  roles?: RoleName[]
}

const navItems = computed(() =>
  ([
    { title: 'Dashboard', to: '/dashboard', icon: 'mdi-view-dashboard-outline' },
    { title: 'Leads', to: '/leads', icon: 'mdi-domain' },
    { title: 'Follow-ups', to: '/followups', icon: 'mdi-calendar-clock-outline' },
    { title: 'Templates', to: '/templates', icon: 'mdi-timeline-outline', roles: ['ADMIN'] },
    { title: 'Reports', to: '/reports', icon: 'mdi-chart-bar', roles: ['ADMIN', 'MANAGER'] },
    { title: 'Users', to: '/users', icon: 'mdi-account-group-outline', roles: ['ADMIN'] },
  ] as NavItem[]).filter((item) => !item.roles || authStore.hasRole(...item.roles)),
)

async function handleLogout() {
  authStore.logout()
  await router.push('/login')
}
</script>

<template>
  <v-app>
    <v-navigation-drawer v-model="drawer" border="0">
      <div class="pa-5">
        <div class="text-overline text-primary">BD CRM</div>
        <div class="text-h6 font-weight-bold">Business Development</div>
      </div>
      <v-list nav>
        <v-list-item
          v-for="item in navItems"
          :key="item.to"
          :prepend-icon="item.icon"
          :title="item.title"
          :to="item.to"
          rounded="lg"
        />
      </v-list>
    </v-navigation-drawer>

    <v-app-bar flat color="transparent">
      <v-app-bar-nav-icon @click="drawer = !drawer" />
      <v-toolbar-title>BD CRM Workspace</v-toolbar-title>
      <v-spacer />
      <div class="text-right mr-4">
        <div class="text-body-2 font-weight-medium">{{ authStore.user?.fullName }}</div>
        <div class="text-caption text-medium-emphasis">{{ authStore.user?.roles.join(', ') }}</div>
      </div>
      <v-btn icon="mdi-logout" variant="text" @click="handleLogout" />
    </v-app-bar>

    <v-main>
      <v-container fluid class="pa-6">
        <RouterView />
      </v-container>
    </v-main>

    <v-snackbar v-model="uiStore.snackbar.open" :color="uiStore.snackbar.color">
      {{ uiStore.snackbar.message }}
    </v-snackbar>
  </v-app>
</template>
