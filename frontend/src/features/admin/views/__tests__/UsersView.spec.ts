import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, ref } from 'vue'
import UsersView from '../UsersView.vue'

const users = ref([
  {
    userId: 1,
    displayName: 'Admin User',
    email: 'admin@example.com',
    phone: '11111111',
    isActive: true,
    createdAt: '2026-01-01T10:00:00Z',
    roles: [{ roleId: 1, roleName: 'ADMIN' }],
  },
  {
    userId: 2,
    displayName: 'Employee User',
    email: 'employee@example.com',
    phone: '22222222',
    isActive: true,
    createdAt: '2026-01-02T10:00:00Z',
    roles: [{ roleId: 3, roleName: 'EMPLOYEE' }],
  },
])

const composableState = {
  users,
  isLoading: ref(false),
  error: ref<string | null>(null),
  isCreating: ref(false),
  isUpdating: ref(false),
  isDeleting: ref(false),
  fetchUsers: vi.fn().mockResolvedValue(undefined),
  updateUser: vi.fn().mockResolvedValue(undefined),
  deleteUser: vi.fn().mockResolvedValue(undefined),
  toggleUserStatus: vi.fn().mockResolvedValue(undefined),
  getUserRole: (user: { roles: Array<{ roleName: string }> }) =>
      user.roles[0]?.roleName ?? 'Unknown',
  formatDate: () => '01. jan. 2026',
}

const authState = {
  currentOrg: { orgNumber: 987654321 },
  isAdmin: true,
  email: 'admin@example.com',
}

// Mock client.post for the add-to-org call
vi.mock('@/api/client', () => ({
  client: {
    post: vi.fn().mockResolvedValue({
      data: {
        userId: 99,
        displayName: 'New Person',
        email: 'new.person@example.com',
        isActive: true,
        roles: [{ roleId: 3, roleName: 'EMPLOYEE' }],
      },
    }),
  },
}))

vi.mock('../../composables/useUsers', () => ({
  useUsers: () => composableState,
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => authState,
}))

const BaseModalStub = defineComponent({
  name: 'BaseModal',
  props: {
    open: { type: Boolean, required: true },
    title: { type: String, required: false, default: '' },
  },
  emits: ['close'],
  template: `
    <div v-if="open" class="modal-stub">
      <h2>{{ title }}</h2>
      <slot />
      <slot name="footer" />
    </div>
  `,
})

describe('UsersView', () => {
  beforeEach(() => {
    users.value = [
      {
        userId: 1,
        displayName: 'Admin User',
        email: 'admin@example.com',
        phone: '11111111',
        isActive: true,
        createdAt: '2026-01-01T10:00:00Z',
        roles: [{ roleId: 1, roleName: 'ADMIN' }],
      },
      {
        userId: 2,
        displayName: 'Employee User',
        email: 'employee@example.com',
        phone: '22222222',
        isActive: true,
        createdAt: '2026-01-02T10:00:00Z',
        roles: [{ roleId: 3, roleName: 'EMPLOYEE' }],
      },
    ]
    composableState.fetchUsers.mockClear()
    composableState.updateUser.mockClear()
    composableState.toggleUserStatus.mockClear()
  })

  const mountView = () =>
      mount(UsersView, {
        global: {
          stubs: {
            BaseModal: BaseModalStub,
            BaseSpinner: true,
            ErrorMessage: defineComponent({
              name: 'ErrorMessage',
              props: { message: { type: String, required: true } },
              template: '<div class="error-message">{{ message }}</div>',
            }),
          },
        },
      })

  it('loads users on mount and renders both users', async () => {
    const wrapper = mountView()
    await Promise.resolve()

    expect(composableState.fetchUsers).toHaveBeenCalledWith(987654321)
    expect(wrapper.text()).toContain('Admin User')
    expect(wrapper.text()).toContain('Employee User')
  })

  it('filters users by search query', async () => {
    const wrapper = mountView()
    await Promise.resolve()

    await wrapper.find('.table-search').setValue('employee')
    expect(wrapper.text()).not.toContain('Admin User')
    expect(wrapper.text()).toContain('Employee User')

    await wrapper.find('.table-search').setValue('')
    expect(wrapper.text()).toContain('Admin User')
    expect(wrapper.text()).toContain('Employee User')
  })

  it('filters users by role chip', async () => {
    const wrapper = mountView()
    await Promise.resolve()

    // Click the Admin chip
    const adminChip = wrapper.findAll('.role-chip').find((c) => c.text().includes('Admin'))
    expect(adminChip).toBeDefined()
    await adminChip!.trigger('click')

    expect(wrapper.text()).toContain('Admin User')
    expect(wrapper.text()).not.toContain('Employee User')

    // Click the Employee chip
    const employeeChip = wrapper.findAll('.role-chip').find((c) => c.text().includes('Ansatt'))
    expect(employeeChip).toBeDefined()
    await employeeChip!.trigger('click')

    expect(wrapper.text()).not.toContain('Admin User')
    expect(wrapper.text()).toContain('Employee User')
  })

  it('shows all users when All chip is clicked', async () => {
    const wrapper = mountView()
    await Promise.resolve()

    // First filter to admin only
    const adminChip = wrapper.findAll('.role-chip').find((c) => c.text().includes('Admin'))
    await adminChip!.trigger('click')

    // Then click All
    const allChip = wrapper.findAll('.role-chip').find((c) => c.text().includes('Alle'))
    await allChip!.trigger('click')

    expect(wrapper.text()).toContain('Admin User')
    expect(wrapper.text()).toContain('Employee User')
  })

  it('opens the add team member modal when button is clicked', async () => {
    const wrapper = mountView()
    await Promise.resolve()

    expect(wrapper.find('.modal-stub').exists()).toBe(false)
    await wrapper.find('.btn--primary').trigger('click')
    expect(wrapper.find('.modal-stub').exists()).toBe(true)
    expect(wrapper.find('.modal-stub h2').text()).toBe('Legg til teammedlem')
  })

  it('calls client.post with email and orgNumber when adding a team member', async () => {
    const { client } = await import('@/api/client')
    const wrapper = mountView()
    await Promise.resolve()

    await wrapper.find('.btn--primary').trigger('click')
    await wrapper.find('#addEmail').setValue('new.person@example.com')
    await wrapper.find('.btn--primary').trigger('click')
    await Promise.resolve()

    expect(client.post).toHaveBeenCalledWith(
        '/users/add-to-org',
        expect.objectContaining({
          email: 'new.person@example.com',
          orgNumber: 987654321,
        })
    )
  })

  it('shows error message when user is not found', async () => {
    const { client } = await import('@/api/client')
    vi.mocked(client.post).mockRejectedValueOnce({
      response: { status: 404, data: { message: 'No account found' } },
    })

    const wrapper = mountView()
    await Promise.resolve()

    await wrapper.find('.btn--primary').trigger('click')
    await wrapper.find('#addEmail').setValue('unknown@example.com')
    await wrapper.find('.btn--primary').trigger('click')
    await Promise.resolve()

    expect(wrapper.find('.add-error').exists()).toBe(true)
    expect(wrapper.find('.add-error').text()).toContain('Ingen konto')
  })

  it('shows error when user is already a member', async () => {
    const { client } = await import('@/api/client')
    vi.mocked(client.post).mockRejectedValueOnce({
      response: { status: 409, data: { message: 'Already a member' } },
    })

    const wrapper = mountView()
    await Promise.resolve()

    await wrapper.find('.btn--primary').trigger('click')
    await wrapper.find('#addEmail').setValue('existing@example.com')
    await wrapper.find('.btn--primary').trigger('click')
    await Promise.resolve()

    expect(wrapper.find('.add-error').text()).toContain('allerede medlem')
  })

  it('opens edit modal and submits updated display name', async () => {
    const wrapper = mountView()
    await Promise.resolve()

    const editButton = wrapper
        .findAll('button')
        .find((b) => b.text().trim() === 'Rediger')
    expect(editButton).toBeDefined()

    await editButton!.trigger('click')
    await wrapper.find('#editDisplayName').setValue('Updated Admin')
    await wrapper.find('#editUserForm').trigger('submit')
    await Promise.resolve()

    expect(composableState.updateUser).toHaveBeenCalledWith(
        1,
        987654321,
        expect.objectContaining({ displayName: 'Updated Admin' })
    )
  })

  it('prevents self deactivation and shows feedback immediately', async () => {
    const wrapper = mountView()
    await Promise.resolve()

    const adminRow = wrapper.findAll('tr').find((r) => r.text().includes('Admin User'))
    expect(adminRow).toBeDefined()

    const deactivateBtn = adminRow!
        .findAll('button')
        .find((b) => b.text().trim() === 'Deaktiver')
    expect(deactivateBtn).toBeDefined()

    await deactivateBtn!.trigger('click')

    // Feedback shown immediately — no confirm step for self
    expect(composableState.toggleUserStatus).not.toHaveBeenCalled()
    expect(wrapper.find('.feedback--error').text()).toContain(
        'Du kan ikke deaktivere din egen bruker.'
    )
  })

  it('deactivates another user after confirmation', async () => {
    const wrapper = mountView()
    await Promise.resolve()

    const employeeRow = wrapper.findAll('tr').find((r) => r.text().includes('Employee User'))
    expect(employeeRow).toBeDefined()

    const deactivateBtn = employeeRow!
        .findAll('button')
        .find((b) => b.text().trim() === 'Deaktiver')
    expect(deactivateBtn).toBeDefined()
    await deactivateBtn!.trigger('click')

    // Confirm modal should appear
    const confirmBtn = wrapper.findAll('button').find((b) => b.text().trim() === 'Bekreft')
    expect(confirmBtn).toBeDefined()
    await confirmBtn!.trigger('click')
    await Promise.resolve()

    expect(composableState.toggleUserStatus).toHaveBeenCalledWith(2, 987654321, true)
  })
})