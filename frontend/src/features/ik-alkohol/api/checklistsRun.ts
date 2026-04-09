import axios, { AxiosError } from 'axios'
import { client } from '../../../api/client'
import type { ChecklistRun, GetRunsResult } from '../types'

export async function updateRunItem(
  runId: number,
  itemId: number,
  orgNumber: number,
): Promise<void> {
  await client.put(
    `/checklists/runs/${runId}/items/${itemId}`,
    {
      booleanValue: true,
    },
    {
      params: { orgNumber },
      headers: {
        Accept: 'application/json',
      },
    },
  )
}

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
