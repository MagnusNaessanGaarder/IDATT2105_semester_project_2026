import { client } from '@/api/client'
import type { User } from '@/types'

const MOCK_USERS = [
  {
    id: 1,
    email: 'Tri@gmail.com',
    password: 'Tri',
    name: 'Tri',
    role: 'ADMIN',
  },
  {
    id: 2,
    email: 'surya@everest-sushi.no',
    password: 'Magnus',
    name: 'Netanyahu',
    role: 'EMPLOYEE',
  },
  {
    id: 3,
    email: 'amir@everest-sushi.no',
    password: 'Anine',
    name: 'Helge Hafting',
    role: 'MANAGER',
  },
]

const USE_MOCK = false 

export interface OrganizationRole {
  orgNumber: number
  orgName: string
  role: string
  joinedAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  email: string
  role: string
  organizations: OrganizationRole[]
}

export interface RegisterData {
  fullName: string
  email: string
  phone?: string
  password: string
}

export const authApi = {
  async login(credentials: {
    email: string
    password: string
  }): Promise<AuthResponse> {
    if (USE_MOCK) {
      await new Promise((resolve) => setTimeout(resolve, 500))

      const user = MOCK_USERS.find(
        (u) => u.email === credentials.email && u.password === credentials.password,
      )

      if (!user) {
        throw new Error('Invalid email or password')
      }

      return {
        accessToken: `mock-jwt-${user.id}-${Date.now()}`,
        refreshToken: `mock-refresh-${user.id}-${Date.now()}`,
        email: user.email,
        role: user.role,
        organizations: [{ orgNumber: 937219997, orgName: 'Everest Sushi', role: user.role, joinedAt: new Date().toISOString() }]
      }
    }

    // Real backend call
    const response = await client.post('/auth/login', credentials)
    return response.data
  },

  async register(userData: RegisterData): Promise<AuthResponse> {
    if (USE_MOCK) {
      await new Promise((resolve) => setTimeout(resolve, 500))
      
      if (MOCK_USERS.some(u => u.email === userData.email)) {
        throw new Error('Email is already registered')
      }

      return {
        accessToken: `mock-jwt-new-${Date.now()}`,
        refreshToken: `mock-refresh-new-${Date.now()}`,
        email: userData.email,
        role: 'EMPLOYEE',
        organizations: [{ orgNumber: 937219997, orgName: 'Everest Sushi', role: 'EMPLOYEE', joinedAt: new Date().toISOString() }]
      }
    }

    const response = await client.post('/auth/register', userData)
    return response.data
  },

  async refresh(refreshToken: string): Promise<AuthResponse> {
    if (USE_MOCK) {
      await new Promise((resolve) => setTimeout(resolve, 200))
      
      return {
        accessToken: `mock-jwt-refreshed-${Date.now()}`,
        refreshToken: `mock-refresh-refreshed-${Date.now()}`,
        email: sessionStorage.getItem('email') || 'test@example.com',
        role: sessionStorage.getItem('role') || 'ADMIN',
        organizations: [{ orgNumber: 937219997, orgName: 'Everest Sushi', role: 'ADMIN', joinedAt: new Date().toISOString() }]
      }
    }

    const response = await client.post('/auth/refresh', { refreshToken })
    return response.data
  },

  async logout(): Promise<void> {
    if (USE_MOCK) {
      await new Promise((resolve) => setTimeout(resolve, 200))
      return
    }
    await client.post('/auth/logout')
  },

  async me(): Promise<User> {
    if (USE_MOCK) {
    const storedUser = sessionStorage.getItem('user')
      if (storedUser) {
        return JSON.parse(storedUser)
      }
      throw new Error('Not logged in')
    }
    const response = await client.get('/auth/me')
    return response.data
  },
}
