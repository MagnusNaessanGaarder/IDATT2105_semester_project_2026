 
 
/**
 * JWT Integration E2E Tests
 * Tester komplett JWT-autentisering mellom frontend og backend
 */

describe('JWT Authentication Integration', () => {
  const testUser = {
    email: 'admin2@everest-sushi.no',
    password: 'Test1234!',
    role: 'ADMIN'
  }

  beforeEach(() => {
    // Clear sessionStorage før hver test
    cy.window().then((win) => {
      win.sessionStorage.clear()
    })
  })

  describe('Login Flow', () => {
    it('should successfully login and receive JWT tokens', () => {
      // Gå til login-siden
      cy.visit('/login')
      
      // Fyll inn skjema
      cy.get('input[type="email"]').clear()
      cy.get('input[type="email"]').type(testUser.email)
      cy.get('input[type="password"]').clear()
      cy.get('input[type="password"]').type(testUser.password)
      
      // Klikk login
      cy.get('button[type="submit"]').click()
      
      // Vent på redirect til dashboard
      cy.url().should('not.include', '/login', { timeout: 10000 })
      
      // Verifiser at tokens lagres i sessionStorage
      cy.window().then((win) => {
        expect(win.sessionStorage.getItem('accessToken')).to.not.equal(null)
        expect(win.sessionStorage.getItem('refreshToken')).to.not.equal(null)
        expect(win.sessionStorage.getItem('email')).to.equal(testUser.email)
        expect(win.sessionStorage.getItem('role')).to.equal(testUser.role)
      })
    })

    it('should show error on invalid credentials', () => {
      cy.visit('/login')
      
      // Fyll inn feil credentials
      cy.get('input[type="email"]').clear()
      cy.get('input[type="email"]').type('wrong@example.com')
      cy.get('input[type="password"]').clear()
      cy.get('input[type="password"]').type('wrongpassword')
      
      // Klikk login
      cy.get('button[type="submit"]').click()
      
      // Verifiser at error-message vises
      cy.get('.error-message', { timeout: 10000 }).should('be.visible')
      cy.get('.error-message').should('contain', 'Ugyldig')
      
      // Verifiser at brukeren fortsatt er på login-siden
      cy.url().should('include', '/login')
    })
  })

  describe('Authenticated Requests', () => {
    beforeEach(() => {
      // Login først via API
      cy.login(testUser.email, testUser.password)
    })

    it('should access protected routes when authenticated', () => {
      cy.visit('/admin/brukere')
      cy.url().should('include', '/admin/brukere')
    })

    it('should maintain authentication after page refresh', () => {
      // Verifiser at vi er på dashboard
      cy.visit('/')
      cy.url().should('not.include', '/login')
      
      // Refresh siden
      cy.reload()
      
      // Verifiser at vi fortsatt er autentisert (ikke redirectet til login)
      cy.url().should('not.include', '/login')
      
      // Verifiser at tokens fortsatt finnes
      cy.window().then((win) => {
        expect(win.sessionStorage.getItem('accessToken')).to.not.equal(null)
        expect(win.sessionStorage.getItem('email')).to.equal(testUser.email)
      })
    })
  })

  describe('Logout Flow', () => {
    beforeEach(() => {
      cy.login(testUser.email, testUser.password)
    })

    it('should clear tokens and redirect to login after session clear', () => {
      // Verifiser at vi er innlogget
      cy.window().then((win) => {
        expect(win.sessionStorage.getItem('accessToken')).to.not.equal(null)
      })
      
      // Clear session (simulerer logout)
      cy.clearSession()
      
      // Verifiser at tokens er fjernet
      cy.window().then((win) => {
        expect(win.sessionStorage.getItem('accessToken')).to.equal(null)
        expect(win.sessionStorage.getItem('refreshToken')).to.equal(null)
        expect(win.sessionStorage.getItem('email')).to.equal(null)
        expect(win.sessionStorage.getItem('role')).to.equal(null)
      })
      
      // Prøv å besøke en beskyttet rute
      cy.visit('/')
      
      // Verifiser redirect til login
      cy.url().should('include', '/login')
    })

    it('should prevent access to protected routes after logout', () => {
      // Clear session først
      cy.clearSession()
      
      // Prøv å besøke en beskyttet rute
      cy.visit('/admin/brukere')
      
      // Verifiser redirect til login
      cy.url().should('include', '/login')
    })
  })

  describe('Token Persistence', () => {
    it('should persist tokens in sessionStorage (not localStorage)', () => {
      cy.login(testUser.email, testUser.password)
      
      // Verifiser sessionStorage
      cy.window().then((win) => {
        expect(win.sessionStorage.getItem('accessToken')).to.not.equal(null)
        expect(win.localStorage.getItem('accessToken')).to.equal(null)
      })
    })
  })

  describe('Protected Route Guards', () => {
    it('should redirect to login when accessing protected route without auth', () => {
      // Sørg for at vi ikke er innlogget
      cy.clearSession()
      
      // Prøv å besøke en beskyttet rute
      cy.visit('/')
      
      // Verifiser redirect til login
      cy.url().should('include', '/login')
    })

    it('should redirect to dashboard when accessing login while authenticated', () => {
      // Login først
      cy.login(testUser.email, testUser.password)
      
      // Prøv å besøke login-siden mens innlogget
      cy.visit('/login')
      
      // Verifiser redirect til dashboard
      cy.url().should('not.include', '/login')
    })
  })
})
