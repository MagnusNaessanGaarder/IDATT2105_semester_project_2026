import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../auth'
import { authApi } from '@/features/auth/api'

// Mock authApi
vi.mock('@/features/auth/api', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    refresh: vi.fn(),
    logout: vi.fn(),
    me: vi.fn(),
  },
}))

describe('Auth Store', () => {
  const createJwt = (payload: Record<string, unknown>) => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
    const body = btoa(JSON.stringify(payload))
    return `${header}.${body}.signature`
  }

  beforeEach(() => {
    setActivePinia(createPinia())
    // Clear sessionStorage
    sessionStorage.clear()
    // Reset mocks
    vi.clearAllMocks()
  })

  describe('Initial State', () => {
    it('should have correct initial state', () => {
      const store = useAuthStore()
      
      expect(store.email).toBeNull()
      expect(store.role).toBeNull()
      expect(store.accessToken).toBeNull()
      expect(store.loading).toBe(false)
      expect(store.error).toBeNull()
      expect(store.isAuthenticated).toBe(false)
      expect(store.hasCheckedAuth).toBe(false)
    })

    it('should restore state from sessionStorage', () => {
      sessionStorage.setItem('email', 'test@example.com')
      sessionStorage.setItem('role', 'ADMIN')
      sessionStorage.setItem('accessToken', 'fake-token')
      
      const store = useAuthStore()
      
      expect(store.email).toBe('test@example.com')
      expect(store.role).toBe('ADMIN')
      expect(store.accessToken).toBe('fake-token')
      expect(store.isAuthenticated).toBe(true)
    })
  })

  describe('Login', () => {
    it('should successfully login and store tokens', async () => {
      const mockResponse = {
        accessToken: 'mock-access-token',
        refreshToken: 'mock-refresh-token',
        email: 'admin@everest-sushi.no',
        role: 'ADMIN',
        organizations: [],
      }
      
      vi.mocked(authApi.login).mockResolvedValue(mockResponse)
      
      const store = useAuthStore()
      const credentials = { email: 'admin@everest-sushi.no', password: 'Test1234!' }
      
      await store.login(credentials)
      
      expect(authApi.login).toHaveBeenCalledWith(credentials)
      expect(store.accessToken).toBe('mock-access-token')
      expect(store.email).toBe('admin@everest-sushi.no')
      expect(store.role).toBe('ADMIN')
      expect(store.isAuthenticated).toBe(true)
      expect(sessionStorage.getItem('accessToken')).toBe('mock-access-token')
      expect(sessionStorage.getItem('refreshToken')).toBe('mock-refresh-token')
      expect(sessionStorage.getItem('email')).toBe('admin@everest-sushi.no')
      expect(sessionStorage.getItem('role')).toBe('ADMIN')
    })

    it('should handle login error', async () => {
      const error = new Error('Invalid credentials')
      vi.mocked(authApi.login).mockRejectedValue(error)
      
      const store = useAuthStore()
      const credentials = { email: 'wrong@example.com', password: 'wrong' }
      
      await expect(store.login(credentials)).rejects.toThrow('Invalid credentials')
      expect(store.error).toBeTruthy()
      expect(store.isAuthenticated).toBe(false)
    })
  })

  describe('Register', () => {
    it('should successfully register and store tokens', async () => {
      const mockResponse = {
        accessToken: 'register-access-token',
        refreshToken: 'register-refresh-token',
        email: 'new@everest-sushi.no',
        role: 'EMPLOYEE',
        organizations: [],
      }

      vi.mocked(authApi.register).mockResolvedValue(mockResponse)

      const store = useAuthStore()
      await store.register({
        email: 'new@everest-sushi.no',
        password: 'Test1234!',
        fullName: 'New User',
      })

      expect(store.accessToken).toBe('register-access-token')
      expect(store.email).toBe('new@everest-sushi.no')
      expect(store.role).toBe('EMPLOYEE')
      expect(sessionStorage.getItem('refreshToken')).toBe('register-refresh-token')
    })

    it('should map backend field errors on register failure', async () => {
      vi.mocked(authApi.register).mockRejectedValue({
        response: {
          data: {
            fieldErrors: {
              email: 'Invalid email',
              password: 'Too weak',
            },
          },
        },
      })

      const store = useAuthStore()
      await expect(
        store.register({ email: 'x', password: 'x', fullName: 'Test User' })
      ).rejects.toBeTruthy()

      expect(store.error?.message).toContain('Invalid email')
      expect(store.error?.message).toContain('Too weak')
    })
  })

  describe('Logout', () => {
    it('should clear all auth data', () => {
      const store = useAuthStore()
      
      // Set up authenticated state
      store.accessToken = 'token'
      store.email = 'test@example.com'
      store.role = 'ADMIN'
      sessionStorage.setItem('accessToken', 'token')
      sessionStorage.setItem('refreshToken', 'refresh')
      sessionStorage.setItem('email', 'test@example.com')
      sessionStorage.setItem('role', 'ADMIN')
      
      store.logout()
      
      expect(store.accessToken).toBeNull()
      expect(store.email).toBeNull()
      expect(store.role).toBeNull()
      expect(store.isAuthenticated).toBe(false)
      expect(sessionStorage.getItem('accessToken')).toBeNull()
      expect(sessionStorage.getItem('refreshToken')).toBeNull()
      expect(sessionStorage.getItem('email')).toBeNull()
      expect(sessionStorage.getItem('role')).toBeNull()
    })
  })

  describe('Role Checking', () => {
    it('should correctly check roles', () => {
      const store = useAuthStore()
      store.role = 'ADMIN'
      
      expect(store.hasRole('ADMIN')).toBe(true)
      expect(store.hasRole('MANAGER')).toBe(false)
      expect(store.hasRole('ADMIN', 'MANAGER')).toBe(true)
    })

    it('should return false when no role', () => {
      const store = useAuthStore()
      
      expect(store.hasRole('ADMIN')).toBe(false)
    })
  })

  describe('Computed Properties', () => {
    it('should compute isAdmin correctly', () => {
      const store = useAuthStore()
      
      expect(store.isAdmin).toBe(false)
      
      store.role = 'ADMIN'
      expect(store.isAdmin).toBe(true)
      
      store.role = 'EMPLOYEE'
      expect(store.isAdmin).toBe(false)
    })

    it('should compute userDisplayName from email', () => {
      const store = useAuthStore()
      
      store.email = 'admin@everest-sushi.no'
      expect(store.userDisplayName).toBe('admin')
      
      store.email = null
      expect(store.userDisplayName).toBe('Gjest')
    })

    it('should compute user object correctly', () => {
      const store = useAuthStore()
      
      expect(store.user).toBeNull()
      
      store.email = 'test@example.com'
      store.role = 'ADMIN'
      
      expect(store.user).toEqual({
        id: '',
        name: 'test',
        email: 'test@example.com',
        role: 'ADMIN',
      })
    })
  })

  describe('checkAuth', () => {
    it('returns false and marks checked when no token exists', async () => {
      const store = useAuthStore()

      const result = await store.checkAuth()

      expect(result).toBe(false)
      expect(store.hasCheckedAuth).toBe(true)
      expect(store.isAuthenticated).toBe(false)
    })

    it('returns false for malformed token payload', async () => {
      sessionStorage.setItem('accessToken', 'malformed.token')
      const store = useAuthStore()

      const result = await store.checkAuth()

      expect(result).toBe(false)
      expect(store.hasCheckedAuth).toBe(true)
      expect(store.isAuthenticated).toBe(false)
    })

    it('returns true when token is valid and not expired', async () => {
      sessionStorage.setItem('accessToken', createJwt({ exp: Math.floor(Date.now() / 1000) + 3600 }))
      sessionStorage.setItem('email', 'active@example.com')
      sessionStorage.setItem('role', 'MANAGER')
      const store = useAuthStore()

      const result = await store.checkAuth()

      expect(result).toBe(true)
      expect(store.hasCheckedAuth).toBe(true)
      expect(store.isAuthenticated).toBe(true)
    })

    it('refreshes token when expired and refresh token exists', async () => {
      sessionStorage.setItem('accessToken', createJwt({ exp: Math.floor(Date.now() / 1000) - 10 }))
      sessionStorage.setItem('refreshToken', 'refresh-token')
      vi.mocked(authApi.refresh).mockResolvedValue({
        accessToken: 'fresh-access-token',
        refreshToken: 'fresh-refresh-token',
        email: 'refresh@example.com',
        role: 'ADMIN',
        organizations: [],
      })

      const store = useAuthStore()
      const result = await store.checkAuth()

      expect(result).toBe(true)
      expect(authApi.refresh).toHaveBeenCalledWith('refresh-token')
      expect(store.accessToken).toBe('fresh-access-token')
      expect(store.hasCheckedAuth).toBe(true)
    })

    it('logs out when token is expired and no refresh token exists', async () => {
      sessionStorage.setItem('accessToken', createJwt({ exp: Math.floor(Date.now() / 1000) - 10 }))
      const store = useAuthStore()

      const result = await store.checkAuth()

      expect(result).toBe(false)
      expect(store.isAuthenticated).toBe(false)
      expect(sessionStorage.getItem('accessToken')).toBeNull()
    })
  })
})
