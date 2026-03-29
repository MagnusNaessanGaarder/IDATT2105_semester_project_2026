export class LoginPage {
  private readonly selectors = {
    emailInput: 'input[type="email"]',
    passwordInput: 'input[type="password"]',
    submitButton: 'button[type="submit"]',
    errorMessage: '[data-testid="error-message"], .error-message'
  }

  visit(): void {
    cy.visit('/login')
  }

  fillEmail(email: string): void {
    cy.get(this.selectors.emailInput).type(email)
  }

  fillPassword(password: string): void {
    cy.get(this.selectors.passwordInput).type(password)
  }

  submit(): void {
    cy.get(this.selectors.submitButton).click()
  }

  getErrorMessage(): Cypress.Chainable {
    return cy.get(this.selectors.errorMessage)
  }

  login(email: string, password: string): void {
    this.visit()
    this.fillEmail(email)
    this.fillPassword(password)
    this.submit()
  }
}
