import { describe, expect, it } from 'vitest'
import { normalizeRelativeApiPath } from '../client'

describe('client path normalization', () => {
  it('keeps already relative paths unchanged', () => {
    expect(normalizeRelativeApiPath('/users')).toBe('/users')
    expect(normalizeRelativeApiPath('users')).toBe('users')
  })

  it('normalizes /api/v1-prefixed paths to relative', () => {
    expect(normalizeRelativeApiPath('/api/v1/users')).toBe('/users')
    expect(normalizeRelativeApiPath('api/v1/users')).toBe('/users')
  })

  it('normalizes /api-prefixed paths to relative', () => {
    expect(normalizeRelativeApiPath('/api/users')).toBe('/users')
    expect(normalizeRelativeApiPath('api/users')).toBe('/users')
  })

  it('keeps absolute URLs unchanged', () => {
    expect(normalizeRelativeApiPath('http://localhost:8080/api/v1/users'))
      .toBe('http://localhost:8080/api/v1/users')
  })
})
