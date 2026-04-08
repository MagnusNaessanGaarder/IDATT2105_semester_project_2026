/**
 * User API service for admin user management.
 * Provides CRUD operations for users within an organization.
 */
import { client } from '@/api/client'

export interface Role {
  roleId: number
  roleName: string
  description?: string
  isSystemRole?: boolean
}

export interface User {
  userId: number
  displayName: string
  email: string
  phone?: string
  isActive: boolean
  createdAt?: string
  updatedAt?: string
  roles: Role[]
}

export interface UserCreateRequest {
  displayName: string
  email: string
  phone?: string
  orgNumber: number
  roleIds?: number[]
}

export interface UserUpdateRequest {
  displayName?: string
  email?: string
  phone?: string
  isActive?: boolean
  roleIds?: number[]
}

/**
 * Fetch all users for a given organization.
 * @param orgNumber - The organization number
 * @returns Promise with array of users
 */
export async function getUsers(orgNumber: number): Promise<User[]> {
  const response = await client.get('/api/users', {
    params: { orgNumber },
  })
  return response.data
}

/**
 * Fetch a specific user by ID.
 * @param userId - The user ID
 * @param orgNumber - The organization number
 * @returns Promise with user data
 */
export async function getUser(userId: number, orgNumber: number): Promise<User> {
  const response = await client.get(`/api/users/${userId}`, {
    params: { orgNumber },
  })
  return response.data
}

/**
 * Create a new user.
 * @param userData - The user creation data
 * @returns Promise with created user
 */
export async function createUser(userData: UserCreateRequest): Promise<User> {
  const response = await client.post('/api/users', userData)
  return response.data
}

/**
 * Update an existing user.
 * @param userId - The user ID
 * @param orgNumber - The organization number
 * @param userData - The user update data
 * @returns Promise with updated user
 */
export async function updateUser(
  userId: number,
  orgNumber: number,
  userData: UserUpdateRequest
): Promise<User> {
  const response = await client.put(`/api/users/${userId}`, userData, {
    params: { orgNumber },
  })
  return response.data
}

/**
 * Delete (deactivate) a user.
 * @param userId - The user ID
 * @param orgNumber - The organization number
 * @returns Promise that resolves when deleted
 */
export async function deleteUser(userId: number, orgNumber: number): Promise<void> {
  await client.delete(`/api/users/${userId}`, {
    params: { orgNumber },
  })
}
