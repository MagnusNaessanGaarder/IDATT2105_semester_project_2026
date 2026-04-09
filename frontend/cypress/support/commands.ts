/// <reference types="cypress" />
/* eslint-disable @typescript-eslint/no-namespace */

/**
 * Authentication Commands
 */

declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Login via API call - faster than UI login
       * Stores tokens in sessionStorage
       */
      loginViaAPI(email: string, password: string): Chainable<void>

      /**
       * Alias for loginViaAPI - simpler API
       */
      login(email: string, password: string): Chainable<void>

      /**
       * Clear all session data
       */
      clearSession(): Chainable<void>

      /**
       * Get session storage values
       */
      getSessionToken(): Chainable<string | null>
      getSessionRefreshToken(): Chainable<string | null>
      getSessionEmail(): Chainable<string | null>
      getSessionRole(): Chainable<string | null>
    }
  }
}

// Module-level cache: one real login per email address per spec file run.
// This means the rate limiter (5 attempts/min) is never exhausted regardless
// of how many tests call cy.login() with the same credentials.
const tokenCache: Record<string, {
  accessToken: string
  refreshToken: string
  email: string
  role: string
  organizations: unknown[]
}> = {}

Cypress.Commands.add('loginViaAPI', (email: string, password: string) => {
  const cached = tokenCache[email]

  if (cached) {
    // Reuse previously obtained tokens — no network request needed.
    cy.window().then((win) => {
      win.sessionStorage.setItem('accessToken', cached.accessToken)
      win.sessionStorage.setItem('refreshToken', cached.refreshToken)
      win.sessionStorage.setItem('email', cached.email)
      win.sessionStorage.setItem('role', cached.role)
      win.sessionStorage.setItem('organizations', JSON.stringify(cached.organizations))
    })
    return
  }

  const forwardedIp = `10.0.0.${Math.floor(Math.random() * 200) + 10}`
  cy.request({
    method: 'POST',
    url: `${Cypress.env('apiUrl')}/auth/login`,
    body: { email, password },
    headers: { 'X-Forwarded-For': forwardedIp },
    failOnStatusCode: false,
  }).then((response) => {
    cy.wrap(response.status).should('eq', 200)

    tokenCache[email] = {
      accessToken: response.body.accessToken,
      refreshToken: response.body.refreshToken,
      email: response.body.email,
      role: response.body.role,
      organizations: response.body.organizations ?? [],
    }

    cy.window().then((win) => {
      win.sessionStorage.setItem('accessToken', response.body.accessToken)
      win.sessionStorage.setItem('refreshToken', response.body.refreshToken)
      win.sessionStorage.setItem('email', response.body.email)
      win.sessionStorage.setItem('role', response.body.role)
      win.sessionStorage.setItem('organizations', JSON.stringify(response.body.organizations ?? []))
    })
  })
})

// Alias for loginViaAPI
Cypress.Commands.add('login', (email: string, password: string) => {
  return cy.loginViaAPI(email, password)
})

// Clears only the application's own session keys without invalidating the
// module-level token cache — subsequent cy.login() calls restore tokens from
// cache without hitting the backend rate limiter.
Cypress.Commands.add('clearSession', () => {
  cy.window().then((win) => {
    ;['accessToken', 'refreshToken', 'email', 'role', 'organizations'].forEach((key) => {
      win.sessionStorage.removeItem(key)
    })
  })
})

Cypress.Commands.add('getSessionToken', () => {
  return cy.window().its('sessionStorage').invoke('getItem', 'accessToken')
})

Cypress.Commands.add('getSessionRefreshToken', () => {
  return cy.window().its('sessionStorage').invoke('getItem', 'refreshToken')
})

Cypress.Commands.add('getSessionEmail', () => {
  return cy.window().its('sessionStorage').invoke('getItem', 'email')
})

Cypress.Commands.add('getSessionRole', () => {
  return cy.window().its('sessionStorage').invoke('getItem', 'role')
})

export {}