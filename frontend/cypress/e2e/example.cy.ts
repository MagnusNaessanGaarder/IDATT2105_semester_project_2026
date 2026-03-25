// https://on.cypress.io/api

describe('Application', () => {
  it('visits the app root url', () => {
    cy.visit('/')
    cy.contains('h1', 'IK kontroll')
  })
})
