import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/auth', () => ({
  login: vi.fn(async () => ({
    token: 'test-token',
    user: {
      id: 1,
      username: 'admin',
      fullName: 'Admin User',
      email: 'admin@example.com',
      organizationId: 1,
      organizationName: 'Acme Org',
      organizationSlug: 'acme-org',
      platformRoles: ['PLATFORM_ADMIN'],
      organizationRoles: ['ORG_ADMIN'],
    },
  })),
  getCurrentUser: vi.fn(async () => ({
    id: 1,
    username: 'admin',
    fullName: 'Admin User',
    email: 'admin@example.com',
    organizationId: 1,
    organizationName: 'Acme Org',
    organizationSlug: 'acme-org',
    platformRoles: ['PLATFORM_ADMIN'],
    organizationRoles: ['ORG_ADMIN'],
  })),
}))

describe('auth store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('stores token and user after login', async () => {
    const store = useAuthStore()

    await store.login({ username: 'admin', password: 'password' })

    expect(store.isAuthenticated).toBe(true)
    expect(store.user?.username).toBe('admin')
    expect(localStorage.getItem('bd-crm.token')).toBe('test-token')
  })

  it('bootstraps current user from stored token', async () => {
    localStorage.setItem('bd-crm.token', 'existing-token')
    const store = useAuthStore()

    await store.bootstrap()

    expect(store.initialized).toBe(true)
    expect(store.roles).toContain('PLATFORM_ADMIN')
  })
})
