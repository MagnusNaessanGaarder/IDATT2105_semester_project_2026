// https://on.cypress.io/api

describe('Application', () => {
  it('visits the app root url and redirects to login', () => {
    cy.visit('/')
    // Should redirect to login when not authenticated
    cy.url().should('include', '/login')
    // Check that login form is displayed
    cy.get('input[type="email"]').should('exist')
    cy.get('input[type="password"]').should('exist')
  })
})
