import type { LawItem } from '../types'

const LAW_REFERENCE_VERSION = 1

interface LawReferencePayload {
  version: number
  lawDocumentId: number
  lawTitle: string
}

export interface ParsedLawReference {
  lawDocumentId: number | null
  lawTitle: string | null
}

const isObject = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null

export const serializeLawReference = (law: LawItem): string =>
  JSON.stringify({
    version: LAW_REFERENCE_VERSION,
    lawDocumentId: law.documentId,
    lawTitle: law.name,
  } satisfies LawReferencePayload)

export const parseLawReference = (value: unknown): ParsedLawReference => {
  if (typeof value !== 'string') {
    return {
      lawDocumentId: null,
      lawTitle: null,
    }
  }

  const trimmed = value.trim()
  if (!trimmed) {
    return {
      lawDocumentId: null,
      lawTitle: null,
    }
  }

  try {
    const parsed = JSON.parse(trimmed) as unknown
    if (!isObject(parsed)) {
      return {
        lawDocumentId: null,
        lawTitle: trimmed,
      }
    }

    const lawDocumentId =
      typeof parsed.lawDocumentId === 'number' && Number.isFinite(parsed.lawDocumentId)
        ? parsed.lawDocumentId
        : null
    const lawTitle = typeof parsed.lawTitle === 'string' && parsed.lawTitle.trim()
      ? parsed.lawTitle.trim()
      : null

    return {
      lawDocumentId,
      lawTitle,
    }
  } catch {
    return {
      lawDocumentId: null,
      lawTitle: trimmed,
    }
  }
}
