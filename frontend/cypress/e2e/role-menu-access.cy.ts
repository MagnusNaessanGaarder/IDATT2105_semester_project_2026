describe('Role-based auth, permissions and menu interaction', () => {
  const apiUrl = Cypress.env('apiUrl')

  const users = {
    admin: {
      email: Cypress.env('adminEmail') || 'admin@everest-sushi.no',
      password: Cypress.env('adminPassword') || 'Test1234!',
    },
    manager: {
      email: Cypress.env('managerEmail') || 'manager@everest-sushi.no',
      password: Cypress.env('managerPassword') || 'Test1234!',
    },
    regular: {
      email: Cypress.env('regularEmail') || 'surya@personligmai.com',
      password: Cypress.env('regularPassword') || 'Test1234!',
    },
  }

  const dataErrorPattern = /Kunne ikke hente|Alle .*feilet|Noen .*feilet|Kontroller innlogging/i

  const loginViaUI = (email: string, password: string) => {
    cy.visit('/login')
    cy.get('input[type="email"]').clear()
    cy.get('input[type="email"]').type(email)
    cy.get('input[type="password"]').clear()
    cy.get('input[type="password"]').type(password)
    cy.get('button[type="submit"]').click()
    cy.url({ timeout: 10000 }).should('not.include', '/login')
  }

  const assertValidSessionToken = (email: string, expectedRole?: string) => {
    cy.window().then((win) => {
      const accessToken = win.sessionStorage.getItem('accessToken')
      const refreshToken = win.sessionStorage.getItem('refreshToken')
      const storedEmail = win.sessionStorage.getItem('email')
      const storedRole = win.sessionStorage.getItem('role')

      expect(accessToken, 'access token').to.be.a('string').and.not.be.empty
      expect(accessToken, 'access token looks like jwt').to.match(/^eyJ/)
      expect(refreshToken, 'refresh token').to.be.a('string').and.not.be.empty
      expect(refreshToken, 'refresh token looks like jwt').to.match(/^eyJ/)
      expect(storedEmail).to.equal(email)

      if (expectedRole) {
        expect(storedRole).to.equal(expectedRole)
      } else {
        expect(storedRole).to.be.a('string').and.not.be.empty
      }
    })
  }

  const assertNoDataLoadError = () => {
    cy.get('[aria-label="API-feil"]').should('not.exist')

    cy.get('body').should(($body) => {
      expect($body.text()).not.to.match(dataErrorPattern)
    })
  }

  const getSessionOrgNumber = () => {
    return cy.window().then((win) => {
      const raw = win.sessionStorage.getItem('organizations')
      expect(raw, 'organizations in sessionStorage').to.not.be.null

      const organizations = JSON.parse(raw || '[]') as Array<{ orgNumber?: number }>
      expect(organizations.length, 'has at least one organization').to.be.greaterThan(0)
      expect(organizations[0]?.orgNumber, 'organization number exists').to.be.a('number')

      return organizations[0]?.orgNumber as number
    })
  }

  const getSessionAccessToken = () => {
    return cy.window().then((win) => {
      const accessToken = win.sessionStorage.getItem('accessToken')
      expect(accessToken, 'access token in sessionStorage').to.be.a('string').and.not.be.empty
      return accessToken as string
    })
  }

  beforeEach(() => {
    cy.visit('/login')
    cy.clearSession()
  })

  it('logs in as admin and stores a valid session token', () => {
    loginViaUI(users.admin.email, users.admin.password)
    assertValidSessionToken(users.admin.email, 'ADMIN')
  })

  it('logs in as a regular user and stores a valid session token', () => {
    loginViaUI(users.regular.email, users.regular.password)
    assertValidSessionToken(users.regular.email)
  })

  it('shows admin menu only for admin users', () => {
    cy.login(users.admin.email, users.admin.password)
    cy.visit('/')
    cy.contains('button', 'ADMIN').should('be.visible')

    cy.clearSession()
    cy.login(users.regular.email, users.regular.password)
    cy.visit('/')
    cy.contains('button', 'ADMIN').should('not.exist')
  })

  it('blocks regular users from create/delete and allows admin for temperature points', () => {
    cy.login(users.regular.email, users.regular.password)
    cy.visit('/')

    getSessionOrgNumber().then((orgNumber) => {
      getSessionAccessToken().then((token) => {
        cy.request({
          method: 'POST',
          url: `${apiUrl}/temperature/points?orgNumber=${orgNumber}`,
          headers: { Authorization: `Bearer ${token}` },
          failOnStatusCode: false,
          body: {
            locationId: 1,
            name: `E2E point ${Date.now()}`,
            isActive: true,
          },
        }).its('status').should('eq', 403)
      })
    })

    cy.clearSession()
    cy.login(users.admin.email, users.admin.password)
    cy.visit('/')

    getSessionOrgNumber().then((orgNumber) => {
      getSessionAccessToken().then((token) => {
        const pointName = `E2E admin point ${Date.now()}`

        cy.request({
          method: 'POST',
          url: `${apiUrl}/temperature/points?orgNumber=${orgNumber}`,
          headers: { Authorization: `Bearer ${token}` },
          body: {
            locationId: 1,
            name: pointName,
            isActive: true,
          },
        }).then((createResponse) => {
          expect(createResponse.status).to.equal(201)
          const pointId = createResponse.body.logPointId as number

          cy.request({
            method: 'DELETE',
            url: `${apiUrl}/temperature/points/${pointId}?orgNumber=${orgNumber}`,
            headers: { Authorization: `Bearer ${token}` },
          }).its('status').should('eq', 204)
        })
      })
    })
  })

  it('allows manager-level account to create and delete temperature points', () => {
    cy.login(users.manager.email, users.manager.password)
    cy.visit('/')
    assertValidSessionToken(users.manager.email)

    getSessionOrgNumber().then((orgNumber) => {
      getSessionAccessToken().then((token) => {
        const pointName = `E2E manager point ${Date.now()}`

        cy.request({
          method: 'POST',
          url: `${apiUrl}/temperature/points?orgNumber=${orgNumber}`,
          headers: { Authorization: `Bearer ${token}` },
          body: {
            locationId: 1,
            name: pointName,
            isActive: true,
          },
          failOnStatusCode: false,
        }).then((createResponse) => {
          expect(createResponse.status, 'manager can create').to.equal(201)
          const pointId = createResponse.body.logPointId as number

          cy.request({
            method: 'DELETE',
            url: `${apiUrl}/temperature/points/${pointId}?orgNumber=${orgNumber}`,
            headers: { Authorization: `Bearer ${token}` },
            failOnStatusCode: false,
          }).its('status').should('eq', 204)
        })
      })
    })
  })

  it('as admin can navigate menu routes and use on-screen controls', () => {
    cy.login(users.admin.email, users.admin.password)

    const routeChecks: Array<{
      path: string
      readySelector: string
      interact: () => void
    }> = [
      {
        path: '/rapporter',
        readySelector: '.report-item',
        interact: () => {
          cy.contains('button', 'Månedlig').click()
          cy.get('input[type="search"]').first().type('rapport')
        },
      },
      {
        path: '/dokumenter',
        readySelector: 'tbody tr',
        interact: () => {
          cy.contains('button', 'Alle').click()
          cy.contains('button', 'Last opp dokument').click()
        },
      },
      {
        path: '/varsler',
        readySelector: '.notification-item',
        interact: () => {
          cy.contains('button', 'Marker alle som lest').click()
          cy.contains('button', 'Uleste').click()
          cy.contains('button', 'Alle').click()
        },
      },
      {
        path: '/ikmat/sjekklister',
        readySelector: '.checklist-card',
        interact: () => {
          cy.get('.checklist-head').first().click()
          cy.get('.task-row input[type="checkbox"]').first().click({ force: true })
        },
      },
      {
        path: '/ikmat/temperatur',
        readySelector: 'tbody tr',
        interact: () => {
          cy.get('body').then(($body) => {
            if ($body.find('a:contains("Se avvik")').length > 0) {
              cy.contains('a', 'Se avvik').click()
              cy.url().should('include', '/ikmat/avvik')
              cy.go('back')
            }
          })
        },
      },
      {
        path: '/ikmat/avvik',
        readySelector: '.deviation-list__item',
        interact: () => {
          cy.contains('button', 'Åpne').click()
          cy.get('.deviation-list__item').first().click()
        },
      },
      {
        path: '/ikmat/haccp',
        readySelector: 'tbody tr',
        interact: () => {
          cy.contains('h2', 'Kritiske kontrollpunkter').should('be.visible')
        },
      },
      {
        path: '/alkohol/daglig-kontroll',
        readySelector: '.control-card',
        interact: () => {
          cy.contains('button', 'Fullført').click()
          cy.contains('button', 'Alle').click()
        },
      },
      {
        path: '/alkohol/sertifiseringer',
        readySelector: 'tbody tr',
        interact: () => {
          cy.contains('h2', 'Personell sertifiseringer').should('be.visible')
        },
      },
      {
        path: '/alkohol/regelverk',
        readySelector: '.law-card',
        interact: () => {
          cy.get('.law-card__link').first().invoke('removeAttr', 'target').click()
          cy.go('back')
        },
      },
      {
        path: '/admin/brukere',
        readySelector: 'tbody tr',
        interact: () => {
          cy.contains('button', '+ Ny bruker').click()
          cy.contains('button', 'Rediger').first().click()
        },
      },
      {
        path: '/admin/innstillinger',
        readySelector: '.settings-section',
        interact: () => {
          cy.contains('button', 'Lagre endringer').click()
          cy.get('input[type="checkbox"]').first().click({ force: true })
        },
      },
    ]

    routeChecks.forEach(({ path, readySelector, interact }) => {
      cy.visit(path)
      cy.url().should('include', path)
      assertNoDataLoadError()
      cy.get(readySelector).should('have.length.greaterThan', 0)
      interact()
    })
  })

  it('as admin can add, edit and delete in daily control', () => {
    cy.login(users.admin.email, users.admin.password)
    cy.visit('/alkohol/daglig-kontroll')

    assertNoDataLoadError()
    cy.get('.control-card').should('have.length.greaterThan', 0)

    const testName = `E2E Kontroll ${Date.now()}`
    const editedName = `${testName} (redigert)`

    cy.contains('button', '+ Legg til kontrollpunkt').click()
    cy.get('input[type="text"]').eq(0).clear()
    cy.get('input[type="text"]').eq(0).type(testName)
    cy.get('input[type="text"]').eq(1).clear()
    cy.get('input[type="text"]').eq(1).type('Alkoholloven § 1')
    cy.get('input[type="text"]').eq(2).clear()
    cy.get('input[type="text"]').eq(2).type('E2E Bruker')
    cy.get('input[type="date"]').clear()
    cy.get('input[type="date"]').type('2026-04-06')
    cy.get('input[type="time"]').clear()
    cy.get('input[type="time"]').type('11:30')
    cy.contains('button', 'Lagre').click()

    cy.contains('.control-card__title', testName).should('be.visible')

    cy.contains('.control-card', testName)
        .within(() => {
          cy.get('.options-menu__trigger').click()
          cy.contains('button', 'Rediger').click()
        })

    cy.get('input[type="text"]').eq(0).clear()
    cy.get('input[type="text"]').eq(0).type(editedName)
    cy.contains('button', 'Lagre').click()
    cy.contains('.control-card__title', editedName).should('be.visible')

    cy.contains('.control-card', editedName)
        .within(() => {
          cy.get('.options-menu__trigger').click()
          cy.contains('button', 'Slett').click()
        })

    cy.contains('.control-card__title', editedName).should('not.exist')
  })

  it('refreshes session token and can log out', () => {
    cy.login(users.admin.email, users.admin.password)
    cy.visit('/')

    cy.window().then((win) => {
      const oldToken = 'malformed.token.value'
      win.sessionStorage.setItem('accessToken', oldToken)
    })

    cy.visit('/admin/brukere')

    cy.get('tbody tr', { timeout: 10000 }).should('have.length.greaterThan', 0)

    cy.window().then((win) => {
      const refreshedAccessToken = win.sessionStorage.getItem('accessToken')
      expect(refreshedAccessToken, 'refreshed access token').to.be.a('string').and.not.be.empty
      expect(refreshedAccessToken).to.not.equal('malformed.token.value')
      expect(refreshedAccessToken).to.match(/^eyJ/)
    })

    cy.get('button[aria-label="Logg ut"]').click()
    cy.url().should('include', '/login')

    cy.window().then((win) => {
      expect(win.sessionStorage.getItem('accessToken')).to.be.null
      expect(win.sessionStorage.getItem('refreshToken')).to.be.null
      expect(win.sessionStorage.getItem('email')).to.be.null
      expect(win.sessionStorage.getItem('role')).to.be.null
    })
  })
})