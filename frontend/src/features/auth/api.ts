import { client } from '@/api/client'
import type { User } from '@/types'

// Mock-brukere for testing uten backend
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

// Setter til true for å bruke mock, false for å kalle backend
const USE_MOCK = true

export const authApi = {
  async login(credentials: {
    email: string
    password: string
  }): Promise<{ token: string; user: User }> {
    if (USE_MOCK) {
      // Simuler nettverksforsinkelse
      await new Promise((resolve) => setTimeout(resolve, 500))

      const user = MOCK_USERS.find(
        (u) => u.email === credentials.email && u.password === credentials.password,
      )

      if (!user) {
        throw new Error('Ugyldig e-post eller passord')
      }

      // Returner uten password
      const { password, ...userWithoutPassword } = user

      return {
        token: `mock-jwt-${user.id}-${Date.now()}`,
        user: userWithoutPassword as User,
      }
    }

    // Ekte backend-kall
    const response = await client.post('/auth/login', credentials)
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
      // Hent fra sessionStorage
      const storedUser = sessionStorage.getItem('user')
      if (storedUser) {
        return JSON.parse(storedUser)
      }
      throw new Error('Ikke innlogget')
    }
    const response = await client.get('/auth/me')
    return response.data
  },
}
