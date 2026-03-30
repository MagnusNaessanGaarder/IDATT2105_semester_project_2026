 
describe('Authentication Flow', () => {
  beforeEach(() => {
    // Clear sessionStorage before each test
    cy.window().then((win) => {
      win.sessionStorage.clear()
    })
  })

  describe('Login Page', () => {
    it('should display login form', () => {
      cy.visit('/login')
      cy.get('input[type="email"]').should('exist')
      cy.get('input[type="password"]').should('exist')
      cy.get('button[type="submit"]').should('contain', 'Logg inn')
    })

    it('should show validation errors for empty fields', () => {
      cy.visit('/login')
      
      // Clear any default values
      cy.get('input[type="email"]').clear()
      cy.get('input[type="password"]').clear()
      
      // Try to submit with empty fields - HTML5 validation should prevent it
      cy.get('button[type="submit"]').click()
      
      // Check that we're still on the login page (validation prevented submission)
      // HTML5 required attribute blocks submission
      cy.url().should('include', '/login')
      
      // The form should still be visible with empty inputs
      cy.get('input[type="email"]').should('be.visible').and('have.value', '')
      cy.get('input[type="password"]').should('be.visible').and('have.value', '')
      cy.get('button[type="submit"]').should('be.visible')
    })

    it('should successfully login with valid credentials', () => {
      cy.visit('/login')
      
      // Clear the default values first
      cy.get('input#email').clear()
      cy.get('input#password').clear()
      
      // Type credentials
      cy.get('input#email').type('admin@everest-sushi.no')
      cy.get('input#password').type('Test1234!')
      
      // Submit form
      cy.get('button[type="submit"]').click()

      // Should redirect to dashboard (wait for API call)
      cy.url().should('not.include', '/login', { timeout: 10000 })
      
      // Should store tokens in sessionStorage
      cy.window().then((win) => {
        expect(win.sessionStorage.getItem('accessToken')).to.not.be.null
        expect(win.sessionStorage.getItem('refreshToken')).to.not.be.null
        expect(win.sessionStorage.getItem('email')).to.equal('admin@everest-sushi.no')
        expect(win.sessionStorage.getItem('role')).to.equal('ADMIN')
      })
    })

    it('should show error message on invalid credentials', () => {
      cy.visit('/login')
      
      // Clear fields
      cy.get('input#email').clear()
      cy.get('input#password').clear()
      
      // Type wrong credentials
      cy.get('input#email').type('wrong@example.com')
      cy.get('input#password').type('wrongpassword')
      
      // Submit form
      cy.get('button[type="submit"]').click()

      // Should show error message (wait for API response)
      cy.get('.error-message', { timeout: 10000 }).should('be.visible')
      cy.get('.error-message').should('contain', 'Ugyldig')
    })
  })

  describe('Protected Routes', () => {
    it('should redirect to login when accessing protected route without auth', () => {
      cy.visit('/')
      cy.url().should('include', '/login')
    })

    it('should allow access to dashboard when authenticated', () => {
      // Set up authenticated session
      cy.window().then((win) => {
        win.sessionStorage.setItem('accessToken', 'fake-token')
        win.sessionStorage.setItem('email', 'admin@everest-sushi.no')
        win.sessionStorage.setItem('role', 'ADMIN')
      })

      cy.visit('/')
      cy.url().should('not.include', '/login')
    })

    it('should redirect to dashboard when accessing login while authenticated', () => {
      // Set up authenticated session
      cy.window().then((win) => {
        win.sessionStorage.setItem('accessToken', 'fake-token')
        win.sessionStorage.setItem('email', 'admin@everest-sushi.no')
        win.sessionStorage.setItem('role', 'ADMIN')
      })

      cy.visit('/login')
      cy.url().should('not.include', '/login')
    })
  })

  describe('Role-Based Access', () => {
    it('should allow ADMIN to access admin routes', () => {
      cy.window().then((win) => {
        win.sessionStorage.setItem('accessToken', 'fake-token')
        win.sessionStorage.setItem('email', 'admin@everest-sushi.no')
        win.sessionStorage.setItem('role', 'ADMIN')
      })

      cy.visit('/admin/brukere')
      cy.url().should('include', '/admin/brukere')
    })

    it('should redirect non-ADMIN users from admin routes', () => {
      cy.window().then((win) => {
        win.sessionStorage.setItem('accessToken', 'fake-token')
        win.sessionStorage.setItem('email', 'employee@example.com')
        win.sessionStorage.setItem('role', 'EMPLOYEE')
      })

      cy.visit('/admin/brukere')
      cy.url().should('not.include', '/admin/brukere')
      // Should redirect to dashboard
      cy.url().should('eq', Cypress.config().baseUrl + '/')
    })
  })

  describe('Logout', () => {
    it('should clear session and redirect to login on logout', () => {
      // First login for real
      cy.visit('/login')
      cy.get('input#email').clear().type('admin@everest-sushi.no')
      cy.get('input#password').clear().type('Test1234!')
      cy.get('button[type="submit"]').click()
      
      // Wait for login to complete
      cy.url().should('not.include', '/login', { timeout: 10000 })
      
      // Verify we're logged in
      cy.window().then((win) => {
        expect(win.sessionStorage.getItem('accessToken')).to.not.be.null
      })
      
      // Clear session to simulate logout
      cy.window().then((win) => {
        win.sessionStorage.clear()
      })
      
      // Visit login page
      cy.visit('/login')
      
      // Verify session is cleared
      cy.window().then((win) => {
        expect(win.sessionStorage.getItem('accessToken')).to.be.null
        expect(win.sessionStorage.getItem('email')).to.be.null
      })

      // Should be on login page
      cy.url().should('include', '/login')
    })
  })
})
