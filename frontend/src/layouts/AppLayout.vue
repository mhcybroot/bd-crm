<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import * as notificationApi from '@/api/notifications'
import * as searchApi from '@/api/search'
import type { NotificationResponse, RoleName, SearchItem } from '@/types/api'

const drawer = ref(true)
const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUiStore()
const notifications = ref<NotificationResponse[]>([])
const notificationMenu = ref(false)
const search = ref('')
const searchResults = ref<SearchItem[]>([])
const searchLoading = ref(false)
const searchOpen = ref(false)
const searchError = ref('')
let searchDebounce: number | undefined

type NavItem = {
  title: string
  to: string
  icon: string
  roles?: RoleName[]
}

const navItems = computed(() =>
  ([
    { title: 'Dashboard', to: '/dashboard', icon: 'mdi-view-dashboard-outline' },
    { title: 'Command Center', to: '/command-center', icon: 'mdi-flash-outline' },
    { title: 'Leads', to: '/leads', icon: 'mdi-domain' },
    { title: 'Follow-ups', to: '/followups', icon: 'mdi-calendar-clock-outline' },
    { title: 'Pipeline Board', to: '/board', icon: 'mdi-view-kanban-outline' },
    { title: 'Duplicates', to: '/duplicates', icon: 'mdi-content-copy', roles: ['ADMIN', 'MANAGER'] },
    { title: 'Templates', to: '/templates', icon: 'mdi-timeline-outline', roles: ['ADMIN'] },
    { title: 'Reports', to: '/reports', icon: 'mdi-chart-bar', roles: ['ADMIN', 'MANAGER'] },
    { title: 'Users', to: '/users', icon: 'mdi-account-group-outline', roles: ['ADMIN'] },
  ] as NavItem[]).filter((item) => !item.roles || authStore.hasRole(...item.roles)),
)

async function handleLogout() {
  authStore.logout()
  await router.push('/login')
}

async function loadNotifications() {
  notifications.value = await notificationApi.listNotifications().catch(() => [])
}

async function executeSearch(query: string) {
  searchLoading.value = true
  searchError.value = ''
  try {
    const response = await searchApi.globalSearch({ q: query })
    searchResults.value = [...response.leads, ...response.notes, ...response.activities, ...response.followups].slice(0, 8)
  } catch {
    searchResults.value = []
    searchError.value = 'Search is unavailable right now'
  } finally {
    searchOpen.value = true
    searchLoading.value = false
  }
}

function runSearch() {
  window.clearTimeout(searchDebounce)
  searchDebounce = window.setTimeout(async () => {
    const query = search.value.trim()
    if (!query) {
      searchResults.value = []
      searchError.value = ''
      searchOpen.value = false
      return
    }
    await executeSearch(query)
  }, 250)
}

function handleSearchFocus() {
  if (search.value.trim()) {
    searchOpen.value = true
  }
}

async function openResult(item: SearchItem) {
  search.value = ''
  searchResults.value = []
  searchError.value = ''
  searchOpen.value = false
  if (item.leadId) {
    await router.push(`/leads/${item.leadId}`)
  }
}

watch(search, runSearch)
onMounted(loadNotifications)
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
      <div class="mx-4" style="width: 360px;">
        <v-menu
          v-model="searchOpen"
          :close-on-content-click="false"
          :open-on-click="false"
          :open-on-focus="false"
          location="bottom"
          offset="8"
          content-class="global-search-menu"
        >
          <template #activator="{ props }">
            <v-text-field
              v-bind="props"
              v-model="search"
              density="comfortable"
              hide-details
              variant="outlined"
              placeholder="Search leads, notes, follow-ups"
              prepend-inner-icon="mdi-magnify"
              clearable
              @focus="handleSearchFocus"
            />
          </template>
          <v-card elevation="8" min-width="360">
            <v-list>
              <v-list-item v-if="searchLoading" title="Searching..." />
              <v-list-item v-else-if="searchError" :title="searchError" />
              <v-list-item
                v-for="item in searchResults"
                :key="`${item.type}-${item.id}`"
                :title="item.title"
                :subtitle="item.subtitle || item.type"
                @click="openResult(item)"
              />
              <v-list-item v-if="!searchLoading && !searchError && !searchResults.length" title="No results found" />
            </v-list>
          </v-card>
        </v-menu>
      </div>
      <v-spacer />
      <v-menu v-model="notificationMenu" location="bottom end">
        <template #activator="{ props }">
          <v-btn icon="mdi-bell-outline" variant="text" v-bind="props" />
        </template>
        <v-card min-width="360">
          <v-list>
            <v-list-item
              v-for="notification in notifications.slice(0, 6)"
              :key="notification.id"
              :title="notification.title"
              :subtitle="notification.message"
            />
            <v-list-item v-if="!notifications.length" title="No notifications yet" />
          </v-list>
        </v-card>
      </v-menu>
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
