import axios, { AxiosError } from 'axios'
import { client } from '../../../api/client'
import type { CertificationResult, CertificationRecord, CertificationType } from '../composables/useAlkoholData'

export interface CreateCertificationRequest {
  userId: number
  trainingType: CertificationType
  title: string
  completedAt?: string | null
  expiresAt?: string | null
  notes?: string
}

export interface UpdateCertificationRequest {
  userId?: number
  trainingType?: CertificationType
  title?: string
  completedAt?: string | null
  expiresAt?: string | null
  notes?: string
}

function handleAxiosError(error: unknown): CertificationResult {
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

export async function getCertifications(orgNumber: number): Promise<CertificationResult> {
  try {
    const response = await client.get<CertificationRecord[]>('/training', {
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
    return handleAxiosError(error)
  }
}

export async function createCertification(
  data: CreateCertificationRequest,
  orgNumber: number,
): Promise<CertificationResult> {
  try {
    const response = await client.post<CertificationRecord>('/training', data, {
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
    return handleAxiosError(error)
  }
}

export async function updateCertification(
  id: number,
  data: UpdateCertificationRequest,
  orgNumber: number,
): Promise<CertificationResult> {
  try {
    const response = await client.put<CertificationRecord>(`/training/${id}`, data, {
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
    return handleAxiosError(error)
  }
}

export async function deleteCertification(
  id: number,
  orgNumber: number,
): Promise<CertificationResult> {
  try {
    await client.delete(`/training/${id}`, {
      params: { orgNumber },
    })

    return {
      ok: true,
      data: null,
    }
  } catch (error: unknown) {
    return handleAxiosError(error)
  }
}

export async function completeCertification(
  id: number,
  orgNumber: number,
  certificateDocumentId?: number | null,
): Promise<CertificationResult> {
  try {
    const response = await client.post<CertificationRecord>(
      `/training/${id}/complete`,
      null,
      {
        params: {
          orgNumber,
          ...(certificateDocumentId && { certificateDocumentId }),
        },
        headers: {
          Accept: 'application/json',
        },
      },
    )

    return {
      ok: true,
      data: response.data,
    }
  } catch (error: unknown) {
    return handleAxiosError(error)
  }
}
