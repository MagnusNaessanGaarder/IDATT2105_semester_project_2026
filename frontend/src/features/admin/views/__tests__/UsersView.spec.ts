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
  createError: ref<string | null>(null),
  updateError: ref<string | null>(null),
  deleteError: ref<string | null>(null),
  isCreating: ref(false),
  isUpdating: ref(false),
  isDeleting: ref(false),
  fetchUsers: vi.fn().mockResolvedValue(undefined),
  createUser: vi.fn().mockResolvedValue(undefined),
  updateUser: vi.fn().mockResolvedValue(undefined),
  deleteUser: vi.fn().mockResolvedValue(undefined),
  toggleUserStatus: vi.fn().mockResolvedValue(undefined),
  getUserRole: (user: { roles: Array<{ roleName: string }> }) => user.roles[0]?.roleName ?? 'Unknown',
  formatDate: () => '01. jan. 2026',
}

const authState = {
  currentOrg: { orgNumber: 987654321 },
  isAdmin: true,
  email: 'admin@example.com',
}

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
    composableState.createUser.mockClear()
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

  it('loads users on mount and supports search + role filtering', async () => {
    const wrapper = mountView()
    await Promise.resolve()

    expect(composableState.fetchUsers).toHaveBeenCalledWith(987654321)
    expect(wrapper.text()).toContain('Admin User')
    expect(wrapper.text()).toContain('Employee User')

    await wrapper.find('.table-search').setValue('employee')
    expect(wrapper.text()).not.toContain('Admin User')
    expect(wrapper.text()).toContain('Employee User')

    await wrapper.find('.table-search').setValue('')
    await wrapper.find('.role-select').setValue('ADMIN')
    expect(wrapper.text()).toContain('Admin User')
    expect(wrapper.text()).not.toContain('Employee User')
  })

  it('creates a user from create modal', async () => {
    const wrapper = mountView()
    await Promise.resolve()

    await wrapper.find('.create-btn').trigger('click')
    await wrapper.find('#displayName').setValue('New Person')
    await wrapper.find('#email').setValue('new.person@example.com')
    await wrapper.find('#role').setValue(['3'])
    await wrapper.find('#createUserForm').trigger('submit')

    expect(composableState.createUser).toHaveBeenCalledWith(
      expect.objectContaining({
        displayName: 'New Person',
        email: 'new.person@example.com',
        orgNumber: 987654321,
      })
    )
  })

  it('updates a user from edit modal', async () => {
    const wrapper = mountView()
    await Promise.resolve()

    const editButton = wrapper
      .findAll('button')
      .find((button) => button.text().trim() === 'Rediger')
    expect(editButton).toBeDefined()

    await editButton!.trigger('click')
    await wrapper.find('#editDisplayName').setValue('Updated Employee')
    await wrapper.find('#editUserForm').trigger('submit')

    expect(composableState.updateUser).toHaveBeenCalledWith(
      1,
      987654321,
      expect.objectContaining({ displayName: 'Updated Employee' })
    )
  })

  it('prevents self deactivation and shows feedback message', async () => {
    const wrapper = mountView()
    await Promise.resolve()

    const row = wrapper
      .findAll('tr')
      .find((entry) => entry.text().includes('Admin User'))
    expect(row).toBeDefined()

    const deactivateButton = row!
      .findAll('button')
      .find((button) => button.text().trim() === 'Deaktiver')
    expect(deactivateButton).toBeDefined()

    await deactivateButton!.trigger('click')

    const confirmButton = wrapper
      .findAll('button')
      .find((button) => button.text().trim() === 'Bekreft')
    expect(confirmButton).toBeDefined()
    await confirmButton!.trigger('click')

    expect(composableState.toggleUserStatus).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Du kan ikke deaktivere din egen bruker.')
  })

  it('deactivates another user after confirmation', async () => {
    const wrapper = mountView()
    await Promise.resolve()

    const row = wrapper
      .findAll('tr')
      .find((entry) => entry.text().includes('Employee User'))
    expect(row).toBeDefined()

    const deactivateButton = row!
      .findAll('button')
      .find((button) => button.text().trim() === 'Deaktiver')
    expect(deactivateButton).toBeDefined()
    await deactivateButton!.trigger('click')

    const confirmButton = wrapper
      .findAll('button')
      .find((button) => button.text().trim() === 'Bekreft')
    expect(confirmButton).toBeDefined()
    await confirmButton!.trigger('click')

    expect(composableState.toggleUserStatus).toHaveBeenCalledWith(2, 987654321, true)
  })
})
