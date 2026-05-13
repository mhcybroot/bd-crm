import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { RoleName } from '@/types/api'
import LoginView from '@/views/LoginView.vue'
import DashboardView from '@/views/DashboardView.vue'
import LeadsView from '@/views/LeadsView.vue'
import LeadDetailView from '@/views/LeadDetailView.vue'
import LeadFormView from '@/views/LeadFormView.vue'
import FollowupsView from '@/views/FollowupsView.vue'
import TemplatesView from '@/views/TemplatesView.vue'
import ReportsView from '@/views/ReportsView.vue'
import UsersView from '@/views/UsersView.vue'
import UnauthorizedView from '@/views/UnauthorizedView.vue'
import AppLayout from '@/layouts/AppLayout.vue'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    roles?: RoleName[]
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    { path: '/unauthorized', component: UnauthorizedView },
    {
      path: '/',
      component: AppLayout,
      meta: { requiresAuth: true },
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', component: DashboardView },
        { path: 'leads', component: LeadsView },
        { path: 'leads/new', component: LeadFormView },
        { path: 'leads/:id', component: LeadDetailView },
        { path: 'leads/:id/edit', component: LeadFormView },
        { path: 'followups', component: FollowupsView },
        { path: 'templates', component: TemplatesView, meta: { roles: ['ADMIN'] } },
        { path: 'reports', component: ReportsView, meta: { roles: ['ADMIN', 'MANAGER'] } },
        { path: 'users', component: UsersView, meta: { roles: ['ADMIN'] } },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  if (!authStore.initialized) {
    await authStore.bootstrap()
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return '/login'
  }

  if (to.path === '/login' && authStore.isAuthenticated) {
    return '/dashboard'
  }

  if (to.meta.roles?.length && !authStore.hasRole(...to.meta.roles)) {
    return '/unauthorized'
  }

  return true
})

export default router
