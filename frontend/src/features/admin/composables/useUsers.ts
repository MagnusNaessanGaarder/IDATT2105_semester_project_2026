/**
 * Composable for user management operations.
 * Provides reactive state and CRUD operations for users.
 */
import { ref, computed } from 'vue'
import { useApi } from '@/shared/composables/useApi'
import * as usersApi from '../api/users'
import type { User, UserCreateRequest, UserUpdateRequest } from '../api/users'

export type { User, UserCreateRequest, UserUpdateRequest }

export function useUsers() {
  // List of all users
  const users = ref<User[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  // Use useApi for individual operations
  const createApi = useApi(usersApi.createUser)
  const updateApi = useApi(usersApi.updateUser)
  const deleteApi = useApi(usersApi.deleteUser)

  /**
   * Fetch all users for an organization.
   * @param orgNumber - The organization number
   */
  async function fetchUsers(orgNumber: number): Promise<void> {
    isLoading.value = true
    error.value = null
    try {
      const data = await usersApi.getUsers(orgNumber)
      users.value = data
    } catch (err: unknown) {
      error.value = getActionErrorMessage(err, 'Kunne ikke hente brukere')
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Create a new user.
   * @param userData - The user creation data
   */
  async function createUser(userData: UserCreateRequest): Promise<User> {
    try {
      const newUser = await createApi.execute(userData)
      if (!newUser) {
        throw new Error('Failed to create user')
      }
      users.value.push(newUser)
      return newUser
    } catch (err: unknown) {
      createApi.error.value = getActionErrorMessage(err, 'Kunne ikke opprette bruker')
      throw err
    }
  }

  /**
   * Update an existing user.
   * @param userId - The user ID
   * @param orgNumber - The organization number
   * @param userData - The user update data
   */
  async function updateUser(
    userId: number,
    orgNumber: number,
    userData: UserUpdateRequest
  ): Promise<User> {
    try {
      const updatedUser = await updateApi.execute(userId, orgNumber, userData)
      if (!updatedUser) {
        throw new Error('Failed to update user')
      }
      const index = users.value.findIndex((u) => u.userId === userId)
      if (index !== -1) {
        users.value[index] = updatedUser
      }
      return updatedUser
    } catch (err: unknown) {
      updateApi.error.value = getActionErrorMessage(err, 'Kunne ikke oppdatere bruker')
      throw err
    }
  }

  /**
   * Delete (deactivate) a user.
   * @param userId - The user ID
   * @param orgNumber - The organization number
   */
  async function deleteUser(userId: number, orgNumber: number): Promise<void> {
    try {
      await deleteApi.execute(userId, orgNumber)
      const index = users.value.findIndex((u) => u.userId === userId)
      const target = users.value[index]
      if (index !== -1 && target) {
        target.isActive = false
      }
    } catch (err: unknown) {
      deleteApi.error.value = getActionErrorMessage(err, 'Kunne ikke deaktivere bruker')
      throw err
    }
  }

  /**
   * Toggle user active status.
   * @param userId - The user ID
   * @param orgNumber - The organization number
   * @param currentStatus - Current active status
   */
  async function toggleUserStatus(
    userId: number,
    orgNumber: number,
    currentStatus: boolean
  ): Promise<void> {
    await updateUser(userId, orgNumber, { isActive: !currentStatus })
  }

  // Computed getters
  const activeUsers = computed(() => users.value.filter((u) => u.isActive))
  const inactiveUsers = computed(() => users.value.filter((u) => !u.isActive))

  /**
   * Get role display name from user's roles.
   * @param user - The user
   * @returns Role name or 'Unknown'
   */
  function getUserRole(user: User): string {
    const firstRole = user.roles?.[0]
    if (firstRole) {
      return firstRole.roleName
    }
    return 'Unknown'
  }

  /**
   * Check if user has admin role.
   * @param user - The user
   * @returns true if admin
   */
  function isAdmin(user: User): boolean {
    return user.roles.some((r) => r.roleName === 'ADMIN')
  }

  /**
   * Check if user has manager role.
   * @param user - The user
   * @returns true if manager
   */
  function isManager(user: User): boolean {
    return user.roles.some((r) => r.roleName === 'MANAGER')
  }

  /**
   * Format date for display.
   * @param dateString - ISO date string
   * @returns Formatted date
   */
  function formatDate(dateString?: string): string {
    if (!dateString) return '-'
    const date = new Date(dateString)
    if (Number.isNaN(date.getTime())) return dateString
    return date.toLocaleDateString('nb-NO', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    })
  }

  /**
   * Format datetime for display.
   * @param dateString - ISO date string
   * @returns Formatted datetime
   */
  function formatDateTime(dateString?: string): string {
    if (!dateString) return '-'
    const date = new Date(dateString)
    if (Number.isNaN(date.getTime())) return dateString
    return date.toLocaleString('nb-NO', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  return {
    // State
    users,
    isLoading,
    error,

    // Operations
    fetchUsers,
    createUser,
    updateUser,
    deleteUser,
    toggleUserStatus,

    // Computed
    activeUsers,
    inactiveUsers,

    // Helpers
    getUserRole,
    isAdmin,
    isManager,
    formatDate,
    formatDateTime,

    // Individual API states
    isCreating: createApi.isLoading,
    createError: createApi.error,
    isUpdating: updateApi.isLoading,
    updateError: updateApi.error,
    isDeleting: deleteApi.isLoading,
    deleteError: deleteApi.error,
  }
}
  const getActionErrorMessage = (err: unknown, fallback: string): string => {
    const status = (err as { response?: { status?: number } })?.response?.status
    if (status === 400) return 'Ugyldig forespørsel. Kontroller feltene og prøv igjen.'
    if (status === 403) return 'Du har ikke tilgang til å utføre denne handlingen.'
    if (status === 404) return 'Brukeren ble ikke funnet.'
    if (status === 409) return 'Handling kunne ikke fullføres på grunn av konflikt.'
    const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
    return message ?? fallback
  }
