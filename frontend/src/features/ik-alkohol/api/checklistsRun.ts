import axios, { AxiosError } from 'axios'
import { client } from '../../../api/client'
import type {
  ChecklistRun,
  ChecklistRunApi,
  ChecklistRunItemApi,
  ChecklistTemplateCreatePayload,
  ChecklistTemplateResponse,
  GetRunsResult,
} from '../types'

export async function updateRunItem(
  runId: number,
  itemId: number,
  orgNumber: number,
  booleanValue: boolean,
): Promise<ChecklistRunItemApi> {
  const response = await client.put<ChecklistRunItemApi>(
    `/checklists/runs/${runId}/items/${itemId}`,
    {
      booleanValue,
    },
    {
      params: { orgNumber },
      headers: {
        Accept: 'application/json',
      },
    },
  )

  return response.data
}

export async function updateChecklistTemplate(
  templateId: number,
  payload: ChecklistTemplateCreatePayload,
  orgNumber: number,
): Promise<ChecklistTemplateResponse> {
  const response = await client.put<ChecklistTemplateResponse>(
    `/checklists/templates/${templateId}`,
    payload,
    {
      params: { orgNumber },
      headers: {
        Accept: 'application/json',
      },
    },
  )

  return response.data
}

export async function completeRun(runId: number, orgNumber: number): Promise<void> {
  await client.put(`/checklists/runs/${runId}/complete`, null, {
    params: { orgNumber },
    headers: {
      Accept: 'application/json',
    },
  })
}

export async function uncompleteRun(runId: number, orgNumber: number): Promise<void> {
  await client.put(`/checklists/runs/${runId}/uncomplete`, null, {
    params: { orgNumber },
    headers: {
      Accept: 'application/json',
    },
  })
}

export async function createChecklistTemplate(
  payload: ChecklistTemplateCreatePayload,
  orgNumber: number,
): Promise<ChecklistTemplateResponse> {
  const response = await client.post<ChecklistTemplateResponse>(
    '/checklists/templates',
    payload,
    {
      params: { orgNumber },
      headers: {
        Accept: 'application/json',
      },
    },
  )

  return response.data
}

export async function createRun(
  templateId: number,
  runDate: string,
  orgNumber: number,
): Promise<ChecklistRunApi> {
  const response = await client.post<ChecklistRunApi>(
    '/checklists/runs',
    {
      templateId,
      runDate,
    },
    {
      params: { orgNumber },
      headers: {
        Accept: 'application/json',
      },
    },
  )

  return response.data
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
