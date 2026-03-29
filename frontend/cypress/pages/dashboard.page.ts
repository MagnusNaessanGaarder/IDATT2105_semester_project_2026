export class DashboardPage {
  private readonly selectors = {
    logoutButton: '[data-testid="logout-btn"], button:contains("Logg ut")',
    userEmail: '[data-testid="user-email"]',
    userRole: '[data-testid="user-role"]'
  }

  visit(): void {
    cy.visit('/')
  }

  logout(): void {
    cy.get(this.selectors.logoutButton).click()
  }

  getUserEmail(): Cypress.Chainable {
    return cy.get(this.selectors.userEmail)
  }

  getUserRole(): Cypress.Chainable {
    return cy.get(this.selectors.userRole)
  }
}
