import { client } from '@/api/client'
import type { User } from '@/types'

export interface OrganizationRole {
  orgNumber: number
  orgName: string
  contactEmail?: string | null
  contactPhone?: string | null
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
    const response = await client.post('/auth/login', credentials)
    return response.data
  },

  async register(userData: RegisterData): Promise<AuthResponse> {
    const response = await client.post('/auth/register', userData)
    return response.data
  },

  async refresh(refreshToken: string): Promise<AuthResponse> {
    const response = await client.post('/auth/refresh', { refreshToken })
    return response.data
  },

  async logout(): Promise<void> {
    await client.post('/auth/logout')
  },

  async me(): Promise<User> {
    const response = await client.get('/auth/me')
    return response.data
  },
}
