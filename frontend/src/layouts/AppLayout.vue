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
const searchHighlightIndex = ref(-1)
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
    { title: 'Duplicates', to: '/duplicates', icon: 'mdi-content-copy', roles: ['PLATFORM_ADMIN', 'ORG_ADMIN', 'ORG_MANAGER'] },
    { title: 'Organizations', to: '/organizations', icon: 'mdi-office-building-cog-outline', roles: ['PLATFORM_ADMIN'] },
    { title: 'Templates', to: '/templates', icon: 'mdi-timeline-outline', roles: ['PLATFORM_ADMIN', 'ORG_ADMIN'] },
    { title: 'Reports', to: '/reports', icon: 'mdi-chart-bar', roles: ['PLATFORM_ADMIN', 'ORG_ADMIN', 'ORG_MANAGER'] },
    { title: 'Users', to: '/users', icon: 'mdi-account-group-outline', roles: ['PLATFORM_ADMIN', 'ORG_ADMIN'] },
  ] as NavItem[]).filter((item) => !item.roles || authStore.hasRole(...item.roles)),
)

const searchGroups = computed(() => {
  const order = [
    ['LEAD', 'Leads'],
    ['NOTE', 'Notes'],
    ['ACTIVITY', 'Activities'],
    ['FOLLOWUP', 'Follow-ups'],
  ] as const
  return order
    .map(([type, label]) => ({
      type,
      label,
      items: searchResults.value.filter((item) => item.type === type),
    }))
    .filter((group) => group.items.length)
})

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
    searchHighlightIndex.value = searchResults.value.length ? 0 : -1
  } catch {
    searchResults.value = []
    searchError.value = 'Search is unavailable right now'
    searchHighlightIndex.value = -1
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
      searchHighlightIndex.value = -1
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
  searchHighlightIndex.value = -1
  searchOpen.value = false
  if (item.leadId) {
    await router.push(`/leads/${item.leadId}`)
  }
}

function handleSearchKeydown(event: KeyboardEvent) {
  if (!searchOpen.value || (!searchResults.value.length && !searchError.value)) {
    return
  }
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    if (!searchResults.value.length) return
    searchHighlightIndex.value = (searchHighlightIndex.value + 1 + searchResults.value.length) % searchResults.value.length
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    if (!searchResults.value.length) return
    searchHighlightIndex.value = (searchHighlightIndex.value - 1 + searchResults.value.length) % searchResults.value.length
  } else if (event.key === 'Enter') {
    if (searchHighlightIndex.value >= 0 && searchResults.value[searchHighlightIndex.value]) {
      event.preventDefault()
      void openResult(searchResults.value[searchHighlightIndex.value])
    }
  } else if (event.key === 'Escape') {
    searchOpen.value = false
  }
}

watch(search, runSearch)
onMounted(loadNotifications)
</script>

<template>
  <v-app>
    <v-navigation-drawer v-model="drawer" border="0" class="app-drawer">
      <div class="drawer-brand">
        <div class="brand-mark">BD</div>
        <div>
          <div class="text-overline text-primary">BD CRM</div>
          <div class="text-h6 font-weight-bold">Revenue Command</div>
          <div class="text-caption text-medium-emphasis">{{ authStore.user?.organizationName }}</div>
        </div>
      </div>
      <div class="drawer-divider" />
      <v-list nav class="drawer-nav">
        <v-list-item
          v-for="item in navItems"
          :key="item.to"
          :prepend-icon="item.icon"
          :title="item.title"
          :to="item.to"
          rounded="xl"
          class="drawer-item"
        />
      </v-list>
      <template #append>
        <div class="drawer-footer">
          <div class="text-caption text-medium-emphasis">Workspace mode</div>
          <div class="text-body-2 font-weight-bold">{{ authStore.roles.join(' / ') }}</div>
        </div>
      </template>
    </v-navigation-drawer>

    <v-app-bar flat color="transparent" class="app-topbar">
      <v-app-bar-nav-icon class="topbar-nav" @click="drawer = !drawer" />
      <div class="topbar-heading">
        <div class="text-overline text-primary">Workspace</div>
        <v-toolbar-title class="toolbar-title">BD CRM Control Room</v-toolbar-title>
      </div>
      <div class="topbar-search">
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
              class="global-search-input"
              hide-details
              variant="outlined"
              placeholder="Jump to leads, notes, follow-ups"
              prepend-inner-icon="mdi-magnify"
              clearable
              @focus="handleSearchFocus"
              @keydown="handleSearchKeydown"
            />
          </template>
          <v-card elevation="8" min-width="420" class="search-panel">
            <div class="search-panel-header">
              <div>
                <div class="section-kicker mb-1">Global Search</div>
                <div class="text-body-2 text-medium-emphasis">Leads, notes, activities, and follow-ups</div>
              </div>
            </div>
            <v-list class="py-1">
              <v-list-item v-if="searchLoading" title="Searching..." subtitle="Pulling results across your CRM" />
              <v-list-item v-else-if="searchError" :title="searchError" subtitle="Try again in a moment" />
              <template v-for="group in searchGroups" :key="group.type">
                <v-list-subheader>{{ group.label }}</v-list-subheader>
                <v-list-item
                  v-for="item in group.items"
                  :key="`${item.type}-${item.id}`"
                  :class="['search-result-item', { 'search-result-item--active': searchHighlightIndex === searchResults.indexOf(item) }]"
                  :title="item.title"
                  :subtitle="item.subtitle || item.type"
                  @click="openResult(item)"
                  @mouseenter="searchHighlightIndex = searchResults.indexOf(item)"
                />
              </template>
              <v-list-item
                v-if="!searchLoading && !searchError && !searchResults.length"
                title="No results found"
                subtitle="Try a company, contact, source, or follow-up keyword"
              />
            </v-list>
          </v-card>
        </v-menu>
      </div>
      <v-spacer />
      <v-menu v-model="notificationMenu" location="bottom end" content-class="global-search-menu">
        <template #activator="{ props }">
          <v-btn icon="mdi-bell-outline" variant="text" class="topbar-icon" v-bind="props" />
        </template>
        <v-card min-width="360" class="search-panel">
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
      <div class="account-pill mr-4">
        <div class="account-avatar">{{ authStore.user?.fullName?.charAt(0) }}</div>
        <div class="text-right">
          <div class="text-body-2 font-weight-bold">{{ authStore.user?.fullName }}</div>
          <div class="text-caption text-medium-emphasis">{{ authStore.user?.organizationName }}</div>
          <div class="text-caption text-medium-emphasis">{{ authStore.roles.join(', ') }}</div>
        </div>
      </div>
      <v-btn icon="mdi-logout" variant="text" class="topbar-icon" @click="handleLogout" />
    </v-app-bar>

    <v-main>
      <v-container fluid class="app-main-shell">
        <RouterView v-slot="{ Component, route }">
          <transition name="crm-fade" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </RouterView>
      </v-container>
    </v-main>

    <v-snackbar v-model="uiStore.snackbar.open" :color="uiStore.snackbar.color" class="crm-snackbar">
      {{ uiStore.snackbar.message }}
    </v-snackbar>
  </v-app>
</template>

<style scoped>
.app-drawer {
  background:
    linear-gradient(180deg, rgba(17, 24, 39, 0.96), rgba(15, 23, 42, 0.93)),
    radial-gradient(circle at top left, rgba(15, 118, 110, 0.26), transparent 28%);
  color: #f8fafc;
}

.drawer-brand {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 28px 24px 18px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(15, 118, 110, 0.95), rgba(234, 88, 12, 0.86));
  box-shadow: 0 18px 35px rgba(15, 118, 110, 0.26);
  font-weight: 800;
  letter-spacing: 0.08em;
}

.drawer-divider {
  height: 1px;
  margin: 0 22px 12px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.18), transparent);
}

.drawer-nav {
  padding: 8px 14px;
}

.drawer-item {
  margin-bottom: 6px;
  color: rgba(248, 250, 252, 0.8);
}

:deep(.drawer-item.v-list-item--active) {
  color: white;
  background: linear-gradient(135deg, rgba(15, 118, 110, 0.28), rgba(255, 255, 255, 0.08));
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
}

.drawer-footer {
  margin: 16px;
  padding: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.05);
}

.app-topbar {
  padding: 14px 22px;
}

.topbar-nav,
.topbar-icon {
  border: 1px solid rgba(17, 34, 51, 0.1);
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(10px);
}

.topbar-heading {
  display: flex;
  flex-direction: column;
}

.toolbar-title {
  font-family: 'Fraunces', serif;
  font-size: 1.55rem;
  letter-spacing: -0.03em;
}

.topbar-search {
  width: min(460px, 100%);
  margin: 0 20px;
}

.global-search-input :deep(.v-field) {
  background: rgba(255, 255, 255, 0.56);
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.08);
}

.search-panel {
  border: 1px solid rgba(255, 255, 255, 0.55);
  background: linear-gradient(180deg, rgba(255, 253, 248, 0.97), rgba(255, 247, 240, 0.92));
  backdrop-filter: blur(18px);
}

.search-panel-header {
  padding: 18px 18px 4px;
}

.search-result-item {
  border-radius: 16px;
  margin: 4px 8px;
  transition: transform 160ms cubic-bezier(0.22, 1, 0.36, 1), background-color 160ms cubic-bezier(0.22, 1, 0.36, 1);
}

.search-result-item--active {
  transform: translateX(4px);
  background: rgba(15, 118, 110, 0.08);
}

.search-panel :deep(.v-list-subheader) {
  color: var(--crm-text-faint);
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.account-pill {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 10px 8px 8px;
  border: 1px solid rgba(17, 34, 51, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.55);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(12px);
}

.account-avatar {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(15, 118, 110, 0.92), rgba(234, 88, 12, 0.84));
  color: white;
  font-weight: 800;
}

.app-main-shell {
  padding: 18px 24px 28px;
}

@media (max-width: 960px) {
  .app-topbar {
    padding: 10px 12px;
  }

  .topbar-search {
    margin: 0 12px;
    width: auto;
  }

  .account-pill {
    display: none;
  }
}
</style>
