import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/features/auth/api'
import type { User } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const isAuthenticated = ref(false)
  const hasCheckedAuth = ref(false)

  const userRole = computed(() => user.value?.role ?? null)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isManager = computed(() => ['ADMIN', 'MANAGER'].includes(user.value?.role ?? ''))

  async function login(credentials: { email: string; password: string }) {
    const { token, user: userData } = await authApi.login(credentials)
    user.value = userData
    isAuthenticated.value = true
    sessionStorage.setItem('jwt_token', token)
    sessionStorage.setItem('user', JSON.stringify(userData))
  }

  async function logout() {
    await authApi.logout()
    user.value = null
    isAuthenticated.value = false
    sessionStorage.removeItem('jwt_token')
    sessionStorage.removeItem('user')
  }

  async function checkAuth() {
    hasCheckedAuth.value = true
    const token = sessionStorage.getItem('jwt_token')
    const storedUser = sessionStorage.getItem('user')

    if (token && storedUser) {
      try {
        user.value = JSON.parse(storedUser)
        isAuthenticated.value = true
      } catch {
        sessionStorage.removeItem('jwt_token')
        sessionStorage.removeItem('user')
      }
    }
  }

  function hasRole(...roles: string[]) {
    return roles.includes(user.value?.role ?? '')
  }

  return {
    user,
    isAuthenticated,
    hasCheckedAuth,
    userRole,
    isAdmin,
    isManager,
    login,
    logout,
    checkAuth,
    hasRole,
  }
})