import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { flushPromises, mount } from '@vue/test-utils'
import AppLayout from '@/layouts/AppLayout.vue'
import vuetify from '@/plugins/vuetify'

const globalSearchMock = vi.fn()
const listNotificationsMock = vi.fn()

vi.mock('@/api/search', () => ({
  globalSearch: (...args: unknown[]) => globalSearchMock(...args),
}))

vi.mock('@/api/notifications', () => ({
  listNotifications: (...args: unknown[]) => listNotificationsMock(...args),
}))

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div>Home</div>' } },
      { path: '/dashboard', component: { template: '<div>Dashboard</div>' } },
      { path: '/command-center', component: { template: '<div>Command Center</div>' } },
      { path: '/leads', component: { template: '<div>Leads</div>' } },
      { path: '/leads/:id', component: { template: '<div>Lead detail</div>' } },
      { path: '/followups', component: { template: '<div>Follow-ups</div>' } },
      { path: '/board', component: { template: '<div>Board</div>' } },
      { path: '/duplicates', component: { template: '<div>Duplicates</div>' } },
      { path: '/organizations', component: { template: '<div>Organizations</div>' } },
      { path: '/templates', component: { template: '<div>Templates</div>' } },
      { path: '/reports', component: { template: '<div>Reports</div>' } },
      { path: '/users', component: { template: '<div>Users</div>' } },
      { path: '/login', component: { template: '<div>Login</div>' } },
    ],
  })
}

async function renderLayout() {
  localStorage.setItem('bd-crm.token', 'test-token')
  localStorage.setItem('bd-crm.user', JSON.stringify({
    id: 1,
    username: 'admin',
    fullName: 'Admin User',
    email: 'admin@example.com',
    organizationId: 1,
    organizationName: 'Acme Org',
    organizationSlug: 'acme-org',
    platformRoles: ['PLATFORM_ADMIN'],
    organizationRoles: ['ORG_ADMIN'],
  }))

  const router = createTestRouter()
  await router.push('/')
  await router.isReady()

  const wrapper = mount(AppLayout, {
    global: {
      plugins: [createPinia(), router, vuetify],
    },
  })

  await flushPromises()
  return { router, wrapper }
}

describe('AppLayout search', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    globalSearchMock.mockReset()
    listNotificationsMock.mockReset()
    listNotificationsMock.mockResolvedValue([])
    localStorage.clear()
    document.body.innerHTML = ''
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders note and follow-up search results and routes to the parent lead', async () => {
    globalSearchMock.mockResolvedValue({
      leads: [],
      notes: [
        { type: 'NOTE', id: 10, leadId: 42, title: 'Acme Corp - Jane Doe', subtitle: 'Meeting recap' },
      ],
      activities: [],
      followups: [
        { type: 'FOLLOWUP', id: 11, leadId: 42, title: 'Acme Corp - Jane Doe', subtitle: 'Step 1 - COMPLETED - INTERESTED' },
      ],
      attachments: [],
    })

    const { router, wrapper } = await renderLayout()
    const pushSpy = vi.spyOn(router, 'push')
    const input = wrapper.find('input[placeholder="Jump to leads, notes, follow-ups"]')

    await input.setValue('acme')
    await vi.advanceTimersByTimeAsync(250)
    await flushPromises()

    expect(document.body.textContent ?? '').toContain('Meeting recap')
    expect(document.body.textContent ?? '').toContain('Step 1 - COMPLETED - INTERESTED')

    const resultItems = wrapper.findAllComponents({ name: 'VListItem' })
      .filter((component) => component.text().includes('Meeting recap'))
    expect(resultItems.length).toBeGreaterThan(0)

    resultItems[0].vm.$emit('click')
    await flushPromises()

    expect(pushSpy).toHaveBeenCalledWith('/leads/42')
  })

  it('shows the organizations navigation item for platform admins', async () => {
    const { wrapper } = await renderLayout()
    expect(wrapper.text()).toContain('Organizations')
  })

  it('keeps the result panel open to show the empty state when nothing matches', async () => {
    globalSearchMock.mockResolvedValue({
      leads: [],
      notes: [],
      activities: [],
      followups: [],
      attachments: [],
    })

    const { wrapper } = await renderLayout()
    const input = wrapper.find('input[placeholder="Jump to leads, notes, follow-ups"]')

    await input.setValue('missing')
    await vi.advanceTimersByTimeAsync(250)
    await flushPromises()

    expect(document.body.textContent ?? '').toContain('No results found')
  })
})
