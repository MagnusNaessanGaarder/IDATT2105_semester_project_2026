import { beforeEach, describe, expect, it, vi } from 'vitest'

const authState = {
  isAuthenticated: true,
  hasCheckedAuth: true,
  user: { role: 'ADMIN' as 'ADMIN' | 'MANAGER' | 'EMPLOYEE' },
  checkAuth: vi.fn().mockResolvedValue(true),
}

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => authState,
}))

import router from '../index'

describe('router admin access guard', () => {
  beforeEach(async () => {
    authState.isAuthenticated = true
    authState.hasCheckedAuth = true
    authState.user = { role: 'ADMIN' }
    authState.checkAuth.mockReset().mockResolvedValue(true)
    await router.push('/')
  })

  it('redirects unauthenticated users to login', async () => {
    authState.isAuthenticated = false
    authState.hasCheckedAuth = true
    authState.user = { role: 'ADMIN' }

    await router.push('/admin/innstillinger')

    expect(router.currentRoute.value.name).toBe('Login')
    expect(router.currentRoute.value.query.redirect).toBe('/admin/innstillinger')
  })

  it('allows MANAGER to open settings', async () => {
    authState.user = { role: 'MANAGER' }

    await router.push('/admin/innstillinger')

    expect(router.currentRoute.value.name).toBe('Settings')
  })

  it('blocks EMPLOYEE from settings with forbidden page', async () => {
    authState.user = { role: 'EMPLOYEE' }

    await router.push('/admin/innstillinger')

    expect(router.currentRoute.value.name).toBe('Forbidden')
  })

  it('blocks MANAGER from users with forbidden page', async () => {
    authState.user = { role: 'MANAGER' }

    await router.push('/admin/brukere')

    expect(router.currentRoute.value.name).toBe('Forbidden')
  })

  it('allows ADMIN to open users', async () => {
    authState.user = { role: 'ADMIN' }

    await router.push('/admin/brukere')

    expect(router.currentRoute.value.name).toBe('Users')
  })
})
