import axios, { AxiosError } from 'axios'
import { client } from '../../../api/client'

type RunStatus = 'DRAFT' | 'ACTIVE' | 'COMPLETED'

export interface ChecklistRun {
  id: string
  orgNumber: string
  status: RunStatus
  [key: string]: unknown
}

export interface GetRunsSuccess {
  ok: true
  data: ChecklistRun[]
}

export interface GetRunsError {
  ok: false
  error: {
    message: string
    status: number | null
    data: unknown
    contentType: string | null
    authorizationSent: boolean
    url: string | null
  }
}

export type GetRunsResult = GetRunsSuccess | GetRunsError

export async function getRuns(): Promise<GetRunsResult> {
  try {
    const response = await client.get<ChecklistRun[]>('/checklists/runs', {
      params: {
        orgNumber: '937219997',
        status: 'DRAFT',
      },
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
          contentType:
            axiosError.response?.headers?.['content-type'] ?? null,
          authorizationSent: Boolean(
            axiosError.config?.headers?.Authorization
          ),
          url:
            axiosError.config?.baseURL && axiosError.config?.url
              ? `${axiosError.config.baseURL}${axiosError.config.url}`
              : axiosError.config?.url ?? null,
        },
      }
    }

    return {
      ok: false,
      error: {
        message: error instanceof Error ? error.message : 'Unknown error',
        status: null,
        data: null,
        contentType: null,
        authorizationSent: false,
        url: null,
      },
    }
  }
}
