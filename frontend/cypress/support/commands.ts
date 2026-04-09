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

Cypress.Commands.add('loginViaAPI', (email: string, password: string) => {
  cy.session([email, password], () => {
    const forwardedIp = `10.0.0.${Math.floor(Math.random() * 200) + 10}`
    cy.request({
      method: 'POST',
      url: `${Cypress.env('apiUrl')}/auth/login`,
      body: { email, password },
      headers: { 'X-Forwarded-For': forwardedIp },
      failOnStatusCode: false
    }).then((response) => {
      cy.wrap(response.status).should('eq', 200)

      cy.window().then((win) => {
        win.sessionStorage.setItem('accessToken', response.body.accessToken)
        win.sessionStorage.setItem('refreshToken', response.body.refreshToken)
        win.sessionStorage.setItem('email', response.body.email)
        win.sessionStorage.setItem('role', response.body.role)
      })
    })
  })
})

// Alias for loginViaAPI
Cypress.Commands.add('login', (email: string, password: string) => {
  return cy.loginViaAPI(email, password)
})

Cypress.Commands.add('clearSession', () => {
  cy.window().then((win) => {
    win.sessionStorage.clear()
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
