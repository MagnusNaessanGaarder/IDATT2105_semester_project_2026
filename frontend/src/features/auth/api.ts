import { client } from '@/api/client'
import type { User } from '@/types'

// Mock users for testing without backend
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

// ! Setter til true for å bruke mock
// ! Bruker da mock data på client-side og ingen tilkobling mot backend 
const USE_MOCK = false // process.env.USE_MOCK 

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  email: string
  role: string
}

export interface RegisterData {
  fullName: string
  email: string
  phone?: string
  password: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  username: string
  role: string
}

export interface RegisterData {
  username: string
  email: string
  password: string
  fullName?: string
}

export const authApi = {
  async login(credentials: {
    email: string
    password: string
  }): Promise<AuthResponse> {
    if (USE_MOCK) {
      // Simulate network delay
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
        username: user.name,
        role: user.role,
      }
    }

    // Real backend call
    const response = await client.post('/auth/login', credentials)
    return response.data
  },

  async register(userData: RegisterData): Promise<AuthResponse> {
    if (USE_MOCK) {
      await new Promise((resolve) => setTimeout(resolve, 500))
      
      // Check if user already exists
      if (MOCK_USERS.some(u => u.email === userData.email)) {
        throw new Error('Email is already registered')
      }

      return {
        accessToken: `mock-jwt-new-${Date.now()}`,
        refreshToken: `mock-refresh-new-${Date.now()}`,
        username: userData.username,
        role: 'EMPLOYEE',
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
        username: sessionStorage.getItem('username') || 'Tri',
        role: sessionStorage.getItem('role') || 'ADMIN',
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
      // Get from sessionStorage
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
