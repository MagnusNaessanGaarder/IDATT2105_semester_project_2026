/// <reference types="cypress" />
/* eslint-disable jest/no-standalone-expect */

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
    cy.request({
      method: 'POST',
      url: `${Cypress.env('apiUrl')}/auth/login`,
      body: { email, password },
      failOnStatusCode: false
    }).then((response) => {
      expect(response.status).to.equal(200)
      
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
  return cy.window().then((win) => win.sessionStorage.getItem('accessToken'))
})

Cypress.Commands.add('getSessionRefreshToken', () => {
  return cy.window().then((win) => win.sessionStorage.getItem('refreshToken'))
})

Cypress.Commands.add('getSessionEmail', () => {
  return cy.window().then((win) => win.sessionStorage.getItem('email'))
})

Cypress.Commands.add('getSessionRole', () => {
  return cy.window().then((win) => win.sessionStorage.getItem('role'))
})

export {}
