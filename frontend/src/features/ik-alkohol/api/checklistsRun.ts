import axios, { AxiosError } from 'axios'
import { client } from '../../../api/client'

type RunStatus = 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE' | 'CANCELLED'

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

export async function completeRun(runId: number, orgNumber: number): Promise<void> {
  await client.put(`/checklists/runs/${runId}/complete`, null, {
    params: { orgNumber },
    headers: {
      Accept: 'application/json',
    },
  })
}

export async function getRuns(): Promise<GetRunsResult> {
  try {
    const response = await client.get<ChecklistRun[]>('/checklists/runs', {
      params: {
        orgNumber: '937219997',
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
