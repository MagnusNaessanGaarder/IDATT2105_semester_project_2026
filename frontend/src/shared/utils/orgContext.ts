const FALLBACK_ORG_NUMBER = 937219997

interface StoredOrganization {
  orgNumber?: number | string
  org_number?: number | string
}

interface JwtLikePayload {
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
  const explicitKeys = ['orgNumber', 'selectedOrgNumber', 'currentOrgNumber']

  for (const key of explicitKeys) {
    const parsed = parseOrgNumber(sessionStorage.getItem(key))
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
    const payload = parts[1]
    if (!payload) {
      return null
    }

    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padding = '='.repeat((4 - (base64.length % 4)) % 4)
    const decoded = atob(base64 + padding)
    const claims = JSON.parse(decoded) as JwtLikePayload

    return (
      parseOrgNumber(claims.orgNumber) ??
      parseOrgNumber(claims.org_number) ??
      parseOrgNumber(claims.organizationNumber) ??
      parseOrgNumber(claims.organization_number)
    )
  } catch {
    return null
  }
}

export const getOrgNumber = (): number => {
  const fromExplicitSession = readSessionOrgNumber()
  if (fromExplicitSession) {
    console.log('[OrgContext] ✅ Resolved orgNumber from explicit session storage:', fromExplicitSession)
    return fromExplicitSession
  }

  const fromSession = readStoredOrgNumber()
  if (fromSession) {
    console.log('[OrgContext] ✅ Resolved orgNumber from organizations session:', fromSession)
    return fromSession
  }

  const fromToken = readOrgNumberFromToken()
  if (fromToken) {
    console.log('[OrgContext] ✅ Resolved orgNumber from JWT token claims:', fromToken)
    return fromToken
  }

  const fromEnv = parseOrgNumber(import.meta.env.VITE_ORG_NUMBER)
  if (fromEnv) {
    console.log('[OrgContext] ✅ Resolved orgNumber from VITE_ORG_NUMBER env:', fromEnv)
    return fromEnv
  }

  console.warn('[OrgContext] ⚠️  Using FALLBACK orgNumber:', FALLBACK_ORG_NUMBER)
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
