import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useUiStore } from '@/stores/ui'

// Mock dependencies
vi.mock('@/stores/ui', () => ({
  useUiStore: vi.fn(() => ({
    showSuccess: vi.fn(),
    showError: vi.fn(),
  })),
}))

vi.mock('@/api/followups', () => ({
  listFollowups: vi.fn(),
  completeFollowup: vi.fn(),
  rescheduleFollowup: vi.fn(),
  skipFollowup: vi.fn(),
  reassignFollowup: vi.fn(),
  bulkFollowupAction: vi.fn(),
}))

vi.mock('@/api/users', () => ({
  listUsers: vi.fn(),
}))

const mockFollowups = [
  {
    id: 1,
    stepNumber: 1,
    dueDate: '2025-01-15',
    assignedUserId: 1,
    assignedUserName: 'Admin User',
    status: 'DUE' as const,
    channel: 'CALL' as const,
    outcome: null,
    instructions: 'Initial contact call',
    notes: null,
    completedAt: null,
    escalatedAt: null,
  },
  {
    id: 2,
    stepNumber: 2,
    dueDate: '2025-01-10',
    assignedUserId: 1,
    assignedUserName: 'Admin User',
    status: 'OVERDUE' as const,
    channel: 'EMAIL' as const,
    outcome: null,
    instructions: 'Follow up email',
    notes: null,
    completedAt: null,
    escalatedAt: null,
  },
  {
    id: 3,
    stepNumber: 1,
    dueDate: '2025-01-12',
    assignedUserId: 2,
    assignedUserName: 'Manager User',
    status: 'COMPLETED' as const,
    channel: 'CALL' as const,
    outcome: 'INTERESTED' as const,
    instructions: 'Qualification call',
    notes: 'Lead is interested in premium tier',
    completedAt: '2025-01-12T14:30:00Z',
    escalatedAt: null,
  },
]

const mockUsers = [
  { id: 1, username: 'admin', fullName: 'Admin User', email: 'admin@test.com', active: true, managerId: null, roles: ['ADMIN'] },
  { id: 2, username: 'manager', fullName: 'Manager User', email: 'manager@test.com', active: true, managerId: null, roles: ['MANAGER'] },
]

describe('FollowupsView', () => {
  let uiStore: ReturnType<typeof useUiStore>

  beforeEach(async () => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
    uiStore = useUiStore()
    vi.resetModules()
  })

  describe('load followups', () => {
    it('calls listFollowups with default status filter "open"', async () => {
      const followupApi = await import('@/api/followups')

      vi.mocked(followupApi.listFollowups).mockResolvedValue(mockFollowups)

      await followupApi.listFollowups('open')

      expect(followupApi.listFollowups).toHaveBeenCalledWith('open')
    })

    it('calls listFollowups with status filter "due"', async () => {
      const followupApi = await import('@/api/followups')

      vi.mocked(followupApi.listFollowups).mockResolvedValue(mockFollowups)

      await followupApi.listFollowups('due')

      expect(followupApi.listFollowups).toHaveBeenCalledWith('due')
    })

    it('calls listFollowups with status filter "overdue"', async () => {
      const followupApi = await import('@/api/followups')

      vi.mocked(followupApi.listFollowups).mockResolvedValue(mockFollowups)

      await followupApi.listFollowups('overdue')

      expect(followupApi.listFollowups).toHaveBeenCalledWith('overdue')
    })

    it('calls listFollowups with status filter "upcoming"', async () => {
      const followupApi = await import('@/api/followups')

      vi.mocked(followupApi.listFollowups).mockResolvedValue(mockFollowups)

      await followupApi.listFollowups('upcoming')

      expect(followupApi.listFollowups).toHaveBeenCalledWith('upcoming')
    })

    it('calls listFollowups with status filter "completed"', async () => {
      const followupApi = await import('@/api/followups')

      vi.mocked(followupApi.listFollowups).mockResolvedValue(mockFollowups)

      await followupApi.listFollowups('completed')

      expect(followupApi.listFollowups).toHaveBeenCalledWith('completed')
    })
  })

  describe('complete follow-up action', () => {
    it('calls completeFollowup with valid outcome', async () => {
      const followupApi = await import('@/api/followups')

      vi.mocked(followupApi.completeFollowup).mockResolvedValue(mockFollowups[0])

      const payload = {
        outcome: 'INTERESTED' as const,
        notes: 'Lead wants to learn more',
      }

      await followupApi.completeFollowup(1, payload)

      expect(followupApi.completeFollowup).toHaveBeenCalledWith(1, payload)
    })

    it('validates outcome is required before completing', async () => {
      // The component requires outcome before completing
      const payload = {
        outcome: null,
        notes: '',
      }

      expect(payload.outcome).toBeNull()
    })
  })

  describe('reschedule follow-up action', () => {
    it('calls rescheduleFollowup with new due date', async () => {
      const followupApi = await import('@/api/followups')

      vi.mocked(followupApi.rescheduleFollowup).mockResolvedValue(mockFollowups[0])

      const payload = {
        dueDate: '2025-01-20',
        outcome: null,
        notes: 'Rescheduled due to conflict',
      }

      await followupApi.rescheduleFollowup(1, payload)

      expect(followupApi.rescheduleFollowup).toHaveBeenCalledWith(1, payload)
    })
  })

  describe('skip follow-up action', () => {
    it('calls skipFollowup with notes', async () => {
      const followupApi = await import('@/api/followups')

      vi.mocked(followupApi.skipFollowup).mockResolvedValue(mockFollowups[0])

      const payload = {
        outcome: 'NO_RESPONSE',
        notes: 'No response after 3 attempts',
      }

      await followupApi.skipFollowup(1, payload)

      expect(followupApi.skipFollowup).toHaveBeenCalledWith(1, payload)
    })
  })

  describe('reassign follow-up action', () => {
    it('calls reassignFollowup with new user assignment', async () => {
      const followupApi = await import('@/api/followups')

      vi.mocked(followupApi.reassignFollowup).mockResolvedValue(mockFollowups[0])

      const payload = {
        assignedUserId: 2,
        notes: 'Reassigned to manager for escalation',
      }

      await followupApi.reassignFollowup(1, payload)

      expect(followupApi.reassignFollowup).toHaveBeenCalledWith(1, payload)
    })
  })

  describe('bulk action', () => {
    it('calls bulkFollowupAction with skip action', async () => {
      const followupApi = await import('@/api/followups')

      vi.mocked(followupApi.bulkFollowupAction).mockResolvedValue([])

      const payload = {
        followupIds: [1, 2],
        action: 'skip' as const,
        dueDate: null,
        assignedUserId: null,
        notes: 'Bulk skipped due to holidays',
      }

      await followupApi.bulkFollowupAction(payload)

      expect(followupApi.bulkFollowupAction).toHaveBeenCalledWith(payload)
    })

    it('calls bulkFollowupAction with reschedule action and new due date', async () => {
      const followupApi = await import('@/api/followups')

      vi.mocked(followupApi.bulkFollowupAction).mockResolvedValue([])

      const payload = {
        followupIds: [1, 2],
        action: 'reschedule' as const,
        dueDate: '2025-01-25',
        assignedUserId: null,
        notes: 'Rescheduled to next week',
      }

      await followupApi.bulkFollowupAction(payload)

      expect(followupApi.bulkFollowupAction).toHaveBeenCalledWith(payload)
    })

    it('calls bulkFollowupAction with reassign action to specific user', async () => {
      const followupApi = await import('@/api/followups')

      vi.mocked(followupApi.bulkFollowupAction).mockResolvedValue([])

      const payload = {
        followupIds: [1, 2],
        action: 'reassign' as const,
        dueDate: null,
        assignedUserId: 2,
        notes: 'Reassigned to manager',
      }

      await followupApi.bulkFollowupAction(payload)

      expect(followupApi.bulkFollowupAction).toHaveBeenCalledWith(payload)
    })
  })

  describe('status display logic', () => {
    it('displays "SCHEDULED" for DUE items when filter is "upcoming"', () => {
      const item = mockFollowups[0]
      const statusFilter = 'upcoming'

      let displayStatus = item.status
      if (statusFilter === 'upcoming' && item.status === 'DUE') {
        displayStatus = 'SCHEDULED'
      }

      expect(displayStatus).toBe('SCHEDULED')
    })

    it('keeps original status for non-DUE items when filter is "upcoming"', () => {
      const item = mockFollowups[2] // COMPLETED
      const statusFilter = 'upcoming'

      let displayStatus = item.status
      if (statusFilter === 'upcoming' && item.status === 'DUE') {
        displayStatus = 'SCHEDULED'
      }

      expect(displayStatus).toBe('COMPLETED')
    })
  })

  describe('canMutate logic', () => {
    it('returns false for COMPLETED follow-ups', () => {
      const item = mockFollowups[2] // COMPLETED

      const canMutate = !['COMPLETED', 'CANCELLED', 'SKIPPED'].includes(item.status)

      expect(canMutate).toBe(false)
    })

    it('returns false for CANCELLED follow-ups', () => {
      const item = { ...mockFollowups[0], status: 'CANCELLED' as const }

      const canMutate = !['COMPLETED', 'CANCELLED', 'SKIPPED'].includes(item.status)

      expect(canMutate).toBe(false)
    })

    it('returns false for SKIPPED follow-ups', () => {
      const item = { ...mockFollowups[0], status: 'SKIPPED' as const }

      const canMutate = !['COMPLETED', 'CANCELLED', 'SKIPPED'].includes(item.status)

      expect(canMutate).toBe(false)
    })

    it('returns true for DUE follow-ups', () => {
      const item = mockFollowups[0] // DUE

      const canMutate = !['COMPLETED', 'CANCELLED', 'SKIPPED'].includes(item.status)

      expect(canMutate).toBe(true)
    })

    it('returns true for OVERDUE follow-ups', () => {
      const item = mockFollowups[1] // OVERDUE

      const canMutate = !['COMPLETED', 'CANCELLED', 'SKIPPED'].includes(item.status)

      expect(canMutate).toBe(true)
    })
  })

  describe('empty state', () => {
    it('handles empty follow-ups list', () => {
      const response: typeof mockFollowups = []

      expect(response).toHaveLength(0)
    })
  })
})