import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type OrganizationRole } from '@/features/auth/api'

interface AuthError {
  message: string
  code?: string
}

interface JwtPayload {
  exp?: number
}

const getStoredOrganizations = (): OrganizationRole[] => {
  try {
    const stored = sessionStorage.getItem('organizations')
    return stored ? JSON.parse(stored) : []
  } catch {
    return []
  }
}

export const useAuthStore = defineStore('auth', () => {
  const email = ref<string | null>(sessionStorage.getItem('email') || null)
  const role = ref<string | null>(sessionStorage.getItem('role') || null)
  const organizations = ref<OrganizationRole[]>(getStoredOrganizations())
  const accessToken = ref<string | null>(sessionStorage.getItem('accessToken') || null)
  const loading = ref(false)
  const error = ref<AuthError | null>(null)
  const hasCheckedAuth = ref(false)

  const isAuthenticated = computed(() => !!accessToken.value)
  const isAdmin = computed(() => role.value === 'ADMIN')
  const currentOrg = computed(() => organizations.value[0] || null)
  const userDisplayName = computed(() => {
    if (email.value) {
      return email.value.split('@')[0] || 'Gjest'
    }
    return 'Gjest'
  })
  const user = computed(() => {
    if (!email.value || !role.value) return null
    return {
      id: '',
      name: email.value.split('@')[0] || '',
      email: email.value,
      role: role.value as 'ADMIN' | 'MANAGER' | 'EMPLOYEE',
    }
  })

  async function login(credentials: { email: string; password: string }) {
    loading.value = true
    error.value = null

    try {
      const response = await authApi.login(credentials)
      setAuthData(response)
      return response
    } catch (err: any) {
      error.value = parseError(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function register(userData: { email: string; password: string; fullName?: string }) {
    loading.value = true
    error.value = null

    try {
      const response = await authApi.register({
        email: userData.email,
        password: userData.password,
        fullName: userData.fullName || userData.email.split('@')[0] || 'User',
        phone: undefined
      })
      setAuthData(response)
      return response
    } catch (err: any) {
      error.value = parseError(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  function logout() {
    email.value = null
    role.value = null
    organizations.value = []
    accessToken.value = null

    sessionStorage.removeItem('accessToken')
    sessionStorage.removeItem('refreshToken')
    sessionStorage.removeItem('email')
    sessionStorage.removeItem('role')
    sessionStorage.removeItem('organizations')
  }

  async function checkAuth() {
    const token = sessionStorage.getItem('accessToken')
    if (!token) {
      logout()
      hasCheckedAuth.value = true
      return false
    }

    try {
      const payload = decodeJwtPayload(token)
      if (!payload || typeof payload.exp !== 'number') {
        logout()
        hasCheckedAuth.value = true
        return false
      }

      const isExpired = payload.exp * 1000 < Date.now()

      if (isExpired) {
        const refreshToken = sessionStorage.getItem('refreshToken')
        if (refreshToken) {
          const response = await authApi.refresh(refreshToken)
          setAuthData(response)
          hasCheckedAuth.value = true
          return true
        } else {
          logout()
          hasCheckedAuth.value = true
          return false
        }
      }
      hasCheckedAuth.value = true
      return true
    } catch {
      logout()
      hasCheckedAuth.value = true
      return false
    }
  }

  function hasRole(...roles: string[]): boolean {
    if (!role.value) return false
    return roles.includes(role.value)
  }

  function decodeJwtPayload(token: string): JwtPayload | null {
    try {
      const parts = token.split('.')
      if (parts.length !== 3) return null

      const payload = parts[1]
      if (!payload) return null

      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
      const padding = '='.repeat((4 - base64.length % 4) % 4)
      const decoded = atob(base64 + padding)

      return JSON.parse(decoded) as JwtPayload
    } catch {
      return null
    }
  }

  function setAuthData(response: {
    accessToken: string
    refreshToken: string
    email: string
    role: string
    organizations: OrganizationRole[]
  }) {
    accessToken.value = response.accessToken
    email.value = response.email
    role.value = response.role
    organizations.value = response.organizations || []

    sessionStorage.setItem('accessToken', response.accessToken)
    sessionStorage.setItem('refreshToken', response.refreshToken)
    sessionStorage.setItem('email', response.email)
    sessionStorage.setItem('role', response.role)
    sessionStorage.setItem('organizations', JSON.stringify(response.organizations || []))
  }

  function parseError(err: any): AuthError {
    if (err.response?.data?.error) {
      return { message: err.response.data.error }
    }
    if (err.response?.data?.fieldErrors) {
      return { message: Object.values(err.response.data.fieldErrors).join(', ') }
    }
    if (err.message) {
      return { message: err.message }
    }
    return { message: 'Something went wrong. Please try again.' }
  }

  return {
    email,
    role,
    organizations,
    accessToken,
    loading,
    error,
    hasCheckedAuth,
    isAuthenticated,
    isAdmin,
    currentOrg,
    userDisplayName,
    user,
    login,
    register,
    logout,
    checkAuth,
    hasRole,
  }
})
