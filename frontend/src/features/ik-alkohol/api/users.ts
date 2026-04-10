import axios, { AxiosError } from 'axios'
import { client } from '../../../api/client'

export interface UserResponse {
  userId: number
  displayName: string
  email: string
  phone?: string
  isActive: boolean
  roles: {
    roleId: number
    roleName: string
    description?: string
  }[]
}

export interface UsersSuccess {
  ok: true
  data: UserResponse[]
}

export interface UsersFailure {
  ok: false
  error: {
    message: string
    status: number | null
    data: unknown
  }
}

export type UsersResult = UsersSuccess | UsersFailure

export async function getUsers(orgNumber: number): Promise<UsersResult> {
  try {
    const response = await client.get<UserResponse[]>('/users', {
      params: { orgNumber },
      headers: {
        Accept: 'application/json',
      },
    })

    return {
      ok: true,
      data: response.data,
    }
  } catch (error: unknown) {
    if (axios.isAxiosError(error)) {
      const axiosError = error as AxiosError<unknown>
      return {
        ok: false,
        error: {
          message: axiosError.message,
          status: axiosError.response?.status ?? null,
          data: axiosError.response?.data ?? null,
        },
      }
    }

    return {
      ok: false,
      error: {
        message: error instanceof Error ? error.message : 'Unknown error',
        status: null,
        data: null,
      },
    }
  }
}
