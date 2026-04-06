const FALLBACK_ORG_NUMBER = 937219997

interface StoredOrganization {
  orgNumber?: number | string
  org_number?: number | string
}

const parseOrgNumber = (value: unknown): number | null => {
  if (typeof value === 'number' && Number.isFinite(value) && value > 0) {
    return value
  }

  if (typeof value === 'string') {
    const parsed = Number(value)
    if (Number.isFinite(parsed) && parsed > 0) {
      return parsed
    }
  }

  return null
}

const readStoredOrgNumber = (): number | null => {
  try {
    const organizations = sessionStorage.getItem('organizations')
    if (!organizations) {
      return null
    }

    const parsed = JSON.parse(organizations) as StoredOrganization[]
    const first = parsed[0]
    if (!first) {
      return null
    }

    return parseOrgNumber(first.orgNumber ?? first.org_number)
  } catch {
    return null
  }
}

export const getOrgNumber = (): number => {
  const fromSession = readStoredOrgNumber()
  if (fromSession) {
    return fromSession
  }

  const fromEnv = parseOrgNumber(import.meta.env.VITE_ORG_NUMBER)
  if (fromEnv) {
    return fromEnv
  }

  return FALLBACK_ORG_NUMBER
}

export const withOrgNumber = <T extends Record<string, unknown>>(params: T): T & { orgNumber: number } => {
  return {
    ...params,
    orgNumber: getOrgNumber(),
  }
}

export const orgHeaders = (headers: Record<string, string> = {}): Record<string, string> => {
  return {
    ...headers,
    'X-Org-Number': String(getOrgNumber()),
  }
}
