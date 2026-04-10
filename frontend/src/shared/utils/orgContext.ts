const FALLBACK_ORG_NUMBER = 937219997

interface StoredOrganization {
  orgNumber?: number | string
  org_number?: number | string
}

interface JwtPayload {
  orgNumber?: number | string
  org_number?: number | string
  organizationNumber?: number | string
  organization_number?: number | string
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

const readSessionOrgNumber = (): number | null => {
  const keys = ['selectedOrgNumber', 'currentOrgNumber', 'orgNumber']

  for (const key of keys) {
    const value = sessionStorage.getItem(key)
    const parsed = parseOrgNumber(value)
    if (parsed) {
      return parsed
    }
  }

  return null
}

const readOrgNumberFromToken = (): number | null => {
  const token = sessionStorage.getItem('accessToken')
  if (!token) {
    return null
  }

  try {
    const parts = token.split('.')
    if (parts.length !== 3 || !parts[1]) {
      return null
    }

    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padding = '='.repeat((4 - (base64.length % 4)) % 4)
    const decoded = atob(base64 + padding)
    const payload = JSON.parse(decoded) as JwtPayload

    return parseOrgNumber(
      payload.orgNumber ?? payload.org_number ?? payload.organizationNumber ?? payload.organization_number,
    )
  } catch {
    return null
  }
}

export const getOrgNumber = (): number => {
  const fromExplicitSession = readSessionOrgNumber()
  if (fromExplicitSession) {
    return fromExplicitSession
  }

  const fromSession = readStoredOrgNumber()
  if (fromSession) {
    return fromSession
  }

  const fromToken = readOrgNumberFromToken()
  if (fromToken) {
    return fromToken
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
