import { beforeEach, describe, expect, it, vi } from 'vitest'
import { client } from '@/api/client'
import {
  createUser,
  deleteUser,
  getUser,
  getUsers,
  updateUser,
  type User,
  type UserCreateRequest,
  type UserUpdateRequest,
} from '../users'

vi.mock('@/api/client', () => ({
  client: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('admin users API', () => {
  const mockUser: User = {
    userId: 42,
    displayName: 'Test User',
    email: 'test@example.com',
    isActive: true,
    roles: [{ roleId: 1, roleName: 'ADMIN' }],
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('fetches users via relative /users path', async () => {
    vi.mocked(client.get).mockResolvedValue({ data: [mockUser] })

    const result = await getUsers(123456789)

    expect(client.get).toHaveBeenCalledWith('/users', {
      params: { orgNumber: 123456789 },
    })
    expect(result).toEqual([mockUser])
  })

  it('fetches single user via relative /users/{id} path', async () => {
    vi.mocked(client.get).mockResolvedValue({ data: mockUser })

    const result = await getUser(42, 123456789)

    expect(client.get).toHaveBeenCalledWith('/users/42', {
      params: { orgNumber: 123456789 },
    })
    expect(result).toEqual(mockUser)
  })

  it('creates user via relative /users path', async () => {
    const payload: UserCreateRequest = {
      displayName: 'New User',
      email: 'new@example.com',
      orgNumber: 123456789,
      roleIds: [1],
    }
    vi.mocked(client.post).mockResolvedValue({ data: mockUser })

    const result = await createUser(payload)

    expect(client.post).toHaveBeenCalledWith('/users', payload)
    expect(result).toEqual(mockUser)
  })

  it('updates user via relative /users/{id} path', async () => {
    const payload: UserUpdateRequest = {
      displayName: 'Updated User',
      isActive: false,
    }
    vi.mocked(client.put).mockResolvedValue({ data: { ...mockUser, ...payload } })

    const result = await updateUser(42, 123456789, payload)

    expect(client.put).toHaveBeenCalledWith('/users/42', payload, {
      params: { orgNumber: 123456789 },
    })
    expect(result.displayName).toBe('Updated User')
  })

  it('deletes user via relative /users/{id} path', async () => {
    vi.mocked(client.delete).mockResolvedValue({ data: undefined })

    await deleteUser(42, 123456789)

    expect(client.delete).toHaveBeenCalledWith('/users/42', {
      params: { orgNumber: 123456789 },
    })
  })
})
