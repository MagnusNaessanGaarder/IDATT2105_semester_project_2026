import { describe, it, expect, beforeEach, vi } from 'vitest'
import { authApi } from '../api'
import { client } from '@/api/client'

// Mock axios client
vi.mock('@/api/client', () => ({
  client: {
    post: vi.fn(),
    get: vi.fn(),
  },
}))

describe('Auth API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    sessionStorage.clear()
  })

  describe('Login', () => {
    it('should call backend login endpoint with correct data', async () => {
      const mockResponse = {
        data: {
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
          email: 'admin@everest-sushi.no',
          role: 'ADMIN',
        },
      }
      
      vi.mocked(client.post).mockResolvedValue(mockResponse)
      
      const credentials = { email: 'admin@everest-sushi.no', password: 'Test1234!' }
      const result = await authApi.login(credentials)
      
      expect(client.post).toHaveBeenCalledWith('/auth/login', credentials)
      expect(result).toEqual(mockResponse.data)
      expect(result.email).toBe('admin@everest-sushi.no')
      expect(result.role).toBe('ADMIN')
    })

    it('should handle login error', async () => {
      const error = new Error('Network error')
      vi.mocked(client.post).mockRejectedValue(error)
      
      const credentials = { email: 'test@example.com', password: 'wrong' }
      
      await expect(authApi.login(credentials)).rejects.toThrow('Network error')
    })
  })

  describe('Register', () => {
    it('should call backend register endpoint with correct data', async () => {
      const mockResponse = {
        data: {
          accessToken: 'new-access-token',
          refreshToken: 'new-refresh-token',
          email: 'new@example.com',
          role: 'EMPLOYEE',
        },
      }
      
      vi.mocked(client.post).mockResolvedValue(mockResponse)
      
      const userData = {
        fullName: 'New User',
        email: 'new@example.com',
        password: 'Password123!',
        phone: '12345678',
      }
      
      const result = await authApi.register(userData)
      
      expect(client.post).toHaveBeenCalledWith('/auth/register', userData)
      expect(result).toEqual(mockResponse.data)
    })
  })

  describe('Refresh Token', () => {
    it('should call backend refresh endpoint', async () => {
      const mockResponse = {
        data: {
          accessToken: 'new-access-token',
          refreshToken: 'new-refresh-token',
          email: 'admin@everest-sushi.no',
          role: 'ADMIN',
        },
      }
      
      vi.mocked(client.post).mockResolvedValue(mockResponse)
      
      const result = await authApi.refresh('old-refresh-token')
      
      expect(client.post).toHaveBeenCalledWith('/auth/refresh', { refreshToken: 'old-refresh-token' })
      expect(result.accessToken).toBe('new-access-token')
    })
  })

  describe('Logout', () => {
    it('should call backend logout endpoint', async () => {
      vi.mocked(client.post).mockResolvedValue({})
      
      await authApi.logout()
      
      expect(client.post).toHaveBeenCalledWith('/auth/logout')
    })
  })

  describe('Me', () => {
    it('should fetch current user data', async () => {
      const mockUser = {
        id: '1',
        name: 'Admin User',
        email: 'admin@everest-sushi.no',
        role: 'ADMIN',
      }
      
      vi.mocked(client.get).mockResolvedValue({ data: mockUser })
      
      const result = await authApi.me()
      
      expect(client.get).toHaveBeenCalledWith('/auth/me')
      expect(result).toEqual(mockUser)
    })
  })
})
