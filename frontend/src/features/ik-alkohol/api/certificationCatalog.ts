import axios, { AxiosError } from 'axios'
import { client } from '../../../api/client'
import type { CertificationType } from '../composables/useAlkoholData'

export interface CertificationCatalogItem {
  trainingType: CertificationType
  displayName: string
  description: string | null
  sortOrder: number
}

export type CertificationCatalogResult = {
  ok: true
  data: CertificationCatalogItem[]
} | {
  ok: false
  error: {
    message: string
    status: number | null
    data: unknown
  }
}

export async function getCertificationCatalog(orgNumber: number): Promise<CertificationCatalogResult> {
  try {
    const response = await client.get<CertificationCatalogItem[]>('/training/catalog', {
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
