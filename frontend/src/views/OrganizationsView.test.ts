import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import OrganizationsView from '@/views/OrganizationsView.vue'
import vuetify from '@/plugins/vuetify'

const listOrganizationsMock = vi.fn()
const bootstrapOrganizationMock = vi.fn()
const updateOrganizationMock = vi.fn()

vi.mock('@/api/organizations', () => ({
  listOrganizations: (...args: unknown[]) => listOrganizationsMock(...args),
  bootstrapOrganization: (...args: unknown[]) => bootstrapOrganizationMock(...args),
  updateOrganization: (...args: unknown[]) => updateOrganizationMock(...args),
}))

describe('OrganizationsView', () => {
  beforeEach(() => {
    listOrganizationsMock.mockReset()
    bootstrapOrganizationMock.mockReset()
    updateOrganizationMock.mockReset()
    listOrganizationsMock.mockResolvedValue([
      {
        id: 1,
        slug: 'default',
        name: 'Default Organization',
        status: 'ACTIVE',
        timezone: 'UTC',
        locale: 'en-US',
        contactEmail: 'admin@example.com',
        planCode: 'standard',
        dataRetentionDays: 365,
      },
    ])
  })

  it('submits bootstrap organization payload and reloads organizations', async () => {
    bootstrapOrganizationMock.mockResolvedValue({
      organization: {
        id: 2,
        slug: 'acme',
        name: 'Acme',
        status: 'ACTIVE',
        timezone: 'UTC',
        locale: 'en-US',
        contactEmail: 'ops@acme.test',
        planCode: 'standard',
        dataRetentionDays: 365,
      },
      firstAdminUser: {
        id: 9,
        username: 'acme-admin',
        fullName: 'Acme Admin',
        email: 'admin@acme.test',
        active: true,
        managerId: null,
        organizationId: 2,
        organizationName: 'Acme',
        roles: ['ORG_ADMIN'],
      },
    })

    const wrapper = mount(OrganizationsView, {
      attachTo: document.body,
      global: {
        plugins: [createPinia(), vuetify],
      },
    })

    await flushPromises()
    await wrapper.get('[data-testid="open-create-organization"]').trigger('click')
    await flushPromises()
    await setInputValue('[data-testid="bootstrap-org-name"] input', 'Acme')
    await setInputValue('[data-testid="bootstrap-org-slug"] input', 'acme')
    await setInputValue('[data-testid="bootstrap-org-contact-email"] input', 'ops@acme.test')
    await setInputValue('[data-testid="bootstrap-admin-full-name"] input', 'Acme Admin')
    await setInputValue('[data-testid="bootstrap-admin-username"] input', 'acme-admin')
    await setInputValue('[data-testid="bootstrap-admin-email"] input', 'admin@acme.test')
    await setInputValue('[data-testid="bootstrap-admin-password"] input', 'secret123')
    await flushPromises()

    const submitButton = document.body.querySelector('[data-testid="submit-bootstrap-organization"]') as HTMLButtonElement | null
    expect(submitButton).toBeTruthy()
    submitButton?.click()
    await flushPromises()

    expect(bootstrapOrganizationMock).toHaveBeenCalled()
    expect(listOrganizationsMock).toHaveBeenCalledTimes(2)
  })

  it('temporarily blocks an active organization from the list', async () => {
    updateOrganizationMock.mockResolvedValue({
      id: 1,
      slug: 'default',
      name: 'Default Organization',
      status: 'SUSPENDED',
      timezone: 'UTC',
      locale: 'en-US',
      contactEmail: 'admin@example.com',
      planCode: 'standard',
      dataRetentionDays: 365,
    })

    const wrapper = mount(OrganizationsView, {
      attachTo: document.body,
      global: {
        plugins: [createPinia(), vuetify],
      },
    })

    await flushPromises()
    await wrapper.get('[data-testid="toggle-organization-status-1"]').trigger('click')
    await flushPromises()

    expect(updateOrganizationMock).toHaveBeenCalledWith(1, expect.objectContaining({
      status: 'SUSPENDED',
      slug: 'default',
    }))
    expect(listOrganizationsMock).toHaveBeenCalledTimes(2)
  })
})

async function setInputValue(selector: string, value: string) {
  const input = document.body.querySelector(selector) as HTMLInputElement | null
  expect(input).toBeTruthy()
  input!.value = value
  input!.dispatchEvent(new Event('input', { bubbles: true }))
  input!.dispatchEvent(new Event('change', { bubbles: true }))
}
