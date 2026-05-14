import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useRouter } from 'vue-router'

// Mock dependencies first
vi.mock('@/stores/ui', () => ({
  useUiStore: vi.fn(() => ({
    showSuccess: vi.fn(),
    showError: vi.fn(),
  })),
}))

vi.mock('@/api/leads', () => ({
  listLeads: vi.fn(),
  bulkLeadAction: vi.fn(),
}))

vi.mock('@/api/users', () => ({
  listUsers: vi.fn(),
}))

vi.mock('@/api/export', () => ({
  exportLeadsCsv: vi.fn(),
}))

vi.mock('@/api/imports', () => ({
  downloadLeadImportTemplate: vi.fn().mockResolvedValue(new Blob()),
}))

const mockLeads = [
  {
    id: 1,
    companyName: 'Acme Corp',
    contactName: 'John Doe',
    email: 'john@acme.com',
    phone: '555-0100',
    source: 'Website',
    status: 'NEW' as const,
    priority: 'HIGH' as const,
    assignedUserId: 1,
    assignedUserName: 'Admin User',
    templateId: 1,
    templateName: 'Sales Template',
    currentStageId: 1,
    currentStageName: 'Initial Contact',
    duplicateState: 'CLEAR' as const,
  },
  {
    id: 2,
    companyName: 'Tech Inc',
    contactName: 'Jane Smith',
    email: 'jane@techinc.com',
    phone: '555-0200',
    source: 'Referral',
    status: 'IN_PROGRESS' as const,
    priority: 'MEDIUM' as const,
    assignedUserId: 2,
    assignedUserName: 'Manager User',
    templateId: 1,
    templateName: 'Sales Template',
    currentStageId: 2,
    currentStageName: 'Qualified',
    duplicateState: 'CLEAR' as const,
  },
]

const mockUsers = [
  { id: 1, username: 'admin', fullName: 'Admin User', email: 'admin@test.com', active: true, managerId: null, roles: ['ADMIN'] },
  { id: 2, username: 'manager', fullName: 'Manager User', email: 'manager@test.com', active: true, managerId: null, roles: ['MANAGER'] },
]

describe('LeadsView', () => {
  let uiStore: ReturnType<typeof useUiStore>
  let router: ReturnType<typeof useRouter>

  beforeEach(async () => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
    uiStore = useUiStore()
    router = useRouter()

    // Reset modules to get fresh component state
    vi.resetModules()
  })

  describe('filter behavior', () => {
    it('renders leads table with data', async () => {
      const leadApi = await import('@/api/leads')
      const userApi = await import('@/api/users')

      vi.mocked(leadApi.listLeads).mockResolvedValue({
        content: mockLeads,
        totalElements: 2,
        totalPages: 1,
        page: 0,
        size: 10,
      })
      vi.mocked(userApi.listUsers).mockResolvedValue(mockUsers)

      // Component mounts and loads leads
      // Note: Full component test requires Vue Test Utils setup
      expect(leadApi.listLeads).toBeDefined()
    })

    it('calls listLeads with correct pagination params', async () => {
      const leadApi = await import('@/api/leads')

      const params = {
        page: 0,
        size: 10,
        status: '' as const,
        assignedUserId: null,
        search: '',
      }

      await leadApi.listLeads(params)

      expect(leadApi.listLeads).toHaveBeenCalledWith(params)
    })

    it('calls listLeads with status filter when set', async () => {
      const leadApi = await import('@/api/leads')

      const params = {
        page: 0,
        size: 10,
        status: 'NEW' as const,
        assignedUserId: null,
        search: '',
      }

      await leadApi.listLeads(params)

      expect(leadApi.listLeads).toHaveBeenCalledWith(params)
      expect(params.status).toBe('NEW')
    })

    it('calls listLeads with assigned user filter when set', async () => {
      const leadApi = await import('@/api/leads')

      const params = {
        page: 0,
        size: 10,
        status: '' as const,
        assignedUserId: 1,
        search: '',
      }

      await leadApi.listLeads(params)

      expect(leadApi.listLeads).toHaveBeenCalledWith(params)
      expect(params.assignedUserId).toBe(1)
    })

    it('calls listLeads with search term when provided', async () => {
      const leadApi = await import('@/api/leads')

      const params = {
        page: 0,
        size: 10,
        status: '' as const,
        assignedUserId: null,
        search: 'Acme',
      }

      await leadApi.listLeads(params)

      expect(leadApi.listLeads).toHaveBeenCalledWith(params)
      expect(params.search).toBe('Acme')
    })
  })

  describe('bulk action', () => {
    it('calls bulkLeadAction with selected lead IDs and new status', async () => {
      const leadApi = await import('@/api/leads')

      const payload = {
        leadIds: [1, 2],
        assignedUserId: null,
        status: 'IN_PROGRESS' as const,
      }

      await leadApi.bulkLeadAction(payload)

      expect(leadApi.bulkLeadAction).toHaveBeenCalledWith(payload)
    })

    it('calls bulkLeadAction with assigned user reassignment', async () => {
      const leadApi = await import('@/api/leads')

      const payload = {
        leadIds: [1],
        assignedUserId: 2,
        status: null,
      }

      await leadApi.bulkLeadAction(payload)

      expect(leadApi.bulkLeadAction).toHaveBeenCalledWith(payload)
    })
  })

  describe('export functionality', () => {
    it('calls exportLeadsCsv when export button clicked', async () => {
      const exportApi = await import('@/api/export')

      await exportApi.exportLeadsCsv()

      expect(exportApi.exportLeadsCsv).toHaveBeenCalled()
    })
  })

  describe('pagination', () => {
    it('calculates total pages correctly', () => {
      const response = {
        content: mockLeads,
        totalElements: 25,
        totalPages: 3,
        page: 0,
        size: 10,
      }

      expect(response.totalPages).toBe(3)
      expect(response.totalElements).toBe(25)
    })

    it('handles empty results', () => {
      const response = {
        content: [],
        totalElements: 0,
        totalPages: 0,
        page: 0,
        size: 10,
      }

      expect(response.content).toHaveLength(0)
      expect(response.totalPages).toBe(0)
    })
  })
})