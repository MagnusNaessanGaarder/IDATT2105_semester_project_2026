describe('Admin e2e flows', () => {
  const adminEmail = 'admin@everest-sushi.no'
  const orgNumber = 937219997

  const usersPayload = [
    {
      userId: 1,
      displayName: 'Tri Tac Le',
      email: 'admin@everest-sushi.no',
      isActive: true,
      roles: [{ roleId: 1, roleName: 'ADMIN' }],
      createdAt: '2026-01-01T10:00:00Z',
    },
    {
      userId: 2,
      displayName: 'Ansatt One',
      email: 'ansatt.one@example.com',
      isActive: true,
      roles: [{ roleId: 3, roleName: 'EMPLOYEE' }],
      createdAt: '2026-01-02T10:00:00Z',
    },
  ]

  const settingsPayload = {
    orgNumber,
    timezoneName: 'Europe/Oslo',
    localeCode: 'nb-NO',
    enableFoodModule: true,
    enableAlcoholModule: true,
    defaultTempMinC: 0,
    defaultTempMaxC: 4,
    reminderEmailEnabled: true,
    notificationEmail: 'admin@everest-sushi.no',
    displayName: 'Everest Sushi',
    legalName: 'Everest Sushi AS',
    contactEmail: 'kontakt@everest-sushi.no',
    contactPhone: '+47 123 45 678',
    retentionUserMonths: 6,
    retentionAuditMonths: 12,
    createdAt: '2026-01-01T10:00:00Z',
    updatedAt: '2026-01-02T10:00:00Z',
  }

  const auditPayload = [
    {
      auditLogId: 10,
      orgNumber: 937219997,
      actionType: 'UPDATE',
      entityType: 'OrganizationSettings',
      entityId: 937219997,
      oldValuesJson: '{"timezoneName":"UTC"}',
      newValuesJson: '{"timezoneName":"Europe/Oslo"}',
      createdAt: '2026-01-03T10:00:00Z',
      actedByUser: { displayName: 'Tri Tac Le', email: 'admin@everest-sushi.no' },
    },
  ]

  const toBase64Url = (value: string) => {
    return btoa(value).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '')
  }

  const createToken = (role: 'ADMIN' | 'MANAGER' | 'EMPLOYEE') => {
    const header = toBase64Url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
    const payload = toBase64Url(JSON.stringify({
      sub: adminEmail,
      role: `ROLE_${role}`,
      exp: Math.floor(Date.now() / 1000) + 3600,
      orgNumber,
    }))
    return `${header}.${payload}.signature`
  }

  const visitAsRole = (path: string, role: 'ADMIN' | 'MANAGER' | 'EMPLOYEE' = 'ADMIN') => {
    const token = createToken(role)
    cy.visit(path, {
      onBeforeLoad(win) {
        win.sessionStorage.setItem('accessToken', token)
        win.sessionStorage.setItem('refreshToken', 'mock-refresh-token')
        win.sessionStorage.setItem('email', adminEmail)
        win.sessionStorage.setItem('role', role)
        win.sessionStorage.setItem('organizations', JSON.stringify([{
          orgNumber,
          orgName: 'Everest Sushi',
          role,
          joinedAt: '2026-01-01T10:00:00Z',
        }]))
        win.sessionStorage.setItem('orgNumber', String(orgNumber))
        win.sessionStorage.setItem('selectedOrgNumber', String(orgNumber))
        win.sessionStorage.setItem('currentOrgNumber', String(orgNumber))
      },
    })
  }

  const stubUsersApi = () => {
    cy.intercept('GET', '**/api/v1/organizations/*/settings*', settingsPayload).as('getOrgSettingsForUsers')
    cy.intercept('GET', '**/api/v1/users*', usersPayload).as('getUsers')
    cy.intercept('POST', '**/api/v1/users', {
      statusCode: 201,
      body: {
        userId: 3,
        displayName: 'Ny Bruker',
        email: 'ny.bruker@example.com',
        isActive: true,
        roles: [{ roleId: 3, roleName: 'EMPLOYEE' }],
      },
    }).as('createUser')
    cy.intercept('PUT', '**/api/v1/users/*', (req) => {
      req.reply({
        statusCode: 200,
        body: {
          userId: 2,
          displayName: 'Ansatt One',
          email: 'ansatt.one@example.com',
          isActive: false,
          roles: [{ roleId: 3, roleName: 'EMPLOYEE' }],
        },
      })
    }).as('updateUser')
  }

  const stubSettingsAndAuditApi = () => {
    cy.intercept('GET', '**/api/v1/organizations/*/settings*', settingsPayload).as('getSettings')
    cy.intercept('PUT', '**/api/v1/organizations/*/settings*', (req) => {
      expect(req.body).to.deep.include({
        timezoneName: 'UTC',
        localeCode: 'en-US',
        enableFoodModule: true,
        enableAlcoholModule: false,
        reminderEmailEnabled: true,
        notificationEmail: 'alerts+admin@example.com',
        retentionUserMonths: 18,
        retentionAuditMonths: 9,
      })

      req.reply({
        statusCode: 200,
        body: {
          ...settingsPayload,
          ...req.body,
        },
      })
    }).as('saveSettings')
    cy.intercept('GET', '**/api/v1/admin/audit-log*', auditPayload).as('getAudit')
    cy.intercept('GET', '**/api/v1/admin/audit-log/action/*', auditPayload).as('getAuditByAction')
    cy.intercept('GET', '**/api/v1/admin/audit-log/date-range*', auditPayload).as('getAuditByDate')
    cy.intercept('GET', '**/api/v1/admin/audit-log/entity/*/*', auditPayload).as('getAuditByEntity')
  }

  it('renders users page and supports search/filter', () => {
    stubUsersApi()
    visitAsRole('/admin/brukere')
    cy.wait('@getOrgSettingsForUsers')
    cy.wait('@getUsers')
    cy.contains('h1', 'Brukere').should('be.visible')

    cy.get('.table-search').type('ansatt.one')
    cy.contains('Ansatt One').should('be.visible')
    cy.contains('Tri Tac Le').should('not.exist')

    cy.get('.table-search').clear()
    cy.get('.role-select').select('ADMIN')
    cy.contains('Tri Tac Le').should('be.visible')
    cy.contains('Ansatt One').should('not.exist')
  })

  it('renders settings page and saves canonical organization settings fields', () => {
    stubSettingsAndAuditApi()

    visitAsRole('/admin/innstillinger')
    cy.wait('@getSettings')
    cy.wait('@getAudit')
    cy.wait('@getSettings')
    cy.contains('h1', 'Innstillinger').should('be.visible')

    // Verify profile fields are displayed
    cy.get('input#display_name').should('have.value', 'Everest Sushi')
    cy.get('input#legal_name').should('have.value', 'Everest Sushi AS')
    cy.get('input#contact_email').should('have.value', 'kontakt@everest-sushi.no')
    cy.get('input#contact_phone').should('have.value', '+47 123 45 678')

    cy.get('select#locale_code').select('en-US')
    cy.get('select#timezone_name').select('UTC')
    cy.get('input#enable_alcohol_module').uncheck({ force: true })
    cy.get('input#notification_email').clear().type('alerts+admin@example.com')
    cy.get('input#retention_user_months').clear().type('18')
    cy.get('input#retention_audit_months').clear().type('9')
    cy.contains('button', 'Lagre endringer').click()
    cy.wait('@saveSettings')
    cy.contains('Innstillinger lagret').should('be.visible')
  })

  it('saves organization profile fields correctly', () => {
    cy.intercept('GET', '**/api/v1/organizations/*/settings*', settingsPayload).as('getSettings')
    cy.intercept('PUT', '**/api/v1/organizations/*/settings*', (req) => {
      expect(req.body).to.deep.include({
        displayName: 'New Display Name',
        legalName: 'New Legal Name AS',
        contactEmail: 'new@example.com',
        contactPhone: '+47 999 88 777',
      })

      req.reply({
        statusCode: 200,
        body: {
          ...settingsPayload,
          ...req.body,
        },
      })
    }).as('saveSettings')
    cy.intercept('GET', '**/api/v1/admin/audit-log*', auditPayload).as('getAudit')

    visitAsRole('/admin/innstillinger')
    cy.wait('@getSettings')
    cy.wait('@getAudit')

    // Update profile fields
    cy.get('input#display_name').clear().type('New Display Name')
    cy.get('input#legal_name').clear().type('New Legal Name AS')
    cy.get('input#contact_email').clear().type('new@example.com')
    cy.get('input#contact_phone').clear().type('+47 999 88 777')

    cy.contains('button', 'Lagre endringer').click()
    cy.wait('@saveSettings')
    cy.contains('Innstillinger lagret').should('be.visible')
  })

  it('shows audit log with before/after values after settings update', () => {
    const updatedAuditPayload = [
      {
        auditLogId: 11,
        orgNumber: 937219997,
        actionType: 'UPDATE',
        entityType: 'OrganizationSettings',
        entityId: 937219997,
        oldValuesJson: '{"timezoneName":"Europe/Oslo","localeCode":"nb-NO","notificationEmail":"admin@everest-sushi.no"}',
        newValuesJson: '{"timezoneName":"UTC","localeCode":"en-US","notificationEmail":"new@example.com"}',
        createdAt: '2026-01-04T10:00:00Z',
        actedByUser: { displayName: 'Tri Tac Le', email: 'admin@everest-sushi.no' },
      },
    ]

    cy.intercept('GET', '**/api/v1/organizations/*/settings*', settingsPayload).as('getSettings')
    cy.intercept('PUT', '**/api/v1/organizations/*/settings*', (req) => {
      req.reply({
        statusCode: 200,
        body: {
          ...settingsPayload,
          ...req.body,
          updatedAt: '2026-01-04T10:00:00Z',
        },
      })
    }).as('saveSettings')

    // First load shows initial audit
    cy.intercept('GET', '**/api/v1/admin/audit-log*', auditPayload).as('getAudit')

    visitAsRole('/admin/innstillinger')
    cy.wait('@getSettings')
    cy.wait('@getAudit')

    // Update settings
    cy.get('select#locale_code').select('en-US')
    cy.get('select#timezone_name').select('UTC')
    cy.get('input#notification_email').clear().type('new@example.com')

    // After save, intercept audit with updated data
    cy.intercept('GET', '**/api/v1/admin/audit-log*', updatedAuditPayload).as('getUpdatedAudit')

    cy.contains('button', 'Lagre endringer').click()
    cy.wait('@saveSettings')
    cy.wait('@getUpdatedAudit')

    // Verify audit log shows the update
    cy.contains('Revisjonslogg').should('be.visible')

    // Click on the audit entry to expand details
    cy.contains('Endret').first().click()

    // Verify before/after values are displayed
    cy.contains('Tidssone').should('be.visible')
    cy.contains('Europe/Oslo').should('be.visible')  // Old value
    cy.contains('UTC').should('be.visible')  // New value
    cy.contains('nb-NO').should('be.visible')  // Old locale
    cy.contains('en-US').should('be.visible')  // New locale
  })

  it('supports reset changes and reset to defaults', () => {
    stubSettingsAndAuditApi()

    visitAsRole('/admin/innstillinger')
    cy.wait('@getSettings')
    cy.wait('@getAudit')

    // Make some changes
    cy.get('select#locale_code').select('en-US')
    cy.get('input#display_name').clear().type('Changed Name')

    // Verify changes are reflected
    cy.get('select#locale_code').should('have.value', 'en-US')
    cy.get('input#display_name').should('have.value', 'Changed Name')

    // Click reset changes
    cy.contains('button', 'Tilbakestill endringer').click()

    // Confirm the reset
    cy.contains('Bekreft tilbakestilling').should('be.visible')
    cy.contains('button', 'Tilbakestill').click()

    // Wait for reload
    cy.wait('@getSettings')

    // Verify values are back to original
    cy.get('select#locale_code').should('have.value', 'nb-NO')
    cy.get('input#display_name').should('have.value', 'Everest Sushi')
  })

  it('blocks save with inline validation when reminder email is enabled and email is empty', () => {
    stubSettingsAndAuditApi()

    visitAsRole('/admin/innstillinger')
    cy.wait('@getSettings')
    cy.wait('@getAudit')

    cy.get('input#notification_email').clear().blur()
    cy.contains('button', 'Lagre endringer').click()

    cy.contains('E-post er påkrevd når e-postpåminnelser er aktivert.').should('be.visible')
    cy.get('@saveSettings.all').should('have.length', 0)
  })

  it('applies audit action/date/entity filters using correct endpoints', () => {
    stubSettingsAndAuditApi()

    visitAsRole('/admin/innstillinger')
    cy.wait('@getSettings')
    cy.wait('@getAudit')

    cy.get('select[aria-label="Filtrer handlingstype"]').select('UPDATE')
    cy.contains('button', 'Filtrer').click()
    cy.wait('@getAuditByAction')

    cy.get('input[aria-label="Fra dato"]').type('2026-01-01')
    cy.get('input[aria-label="Til dato"]').type('2026-01-31')
    cy.contains('button', 'Filtrer').click()
    cy.wait('@getAuditByDate')

    cy.get('input[aria-label="Entity type"]').clear().type('OrganizationSettings')
    cy.get('input[aria-label="Entity ID"]').clear().type('937219997')
    cy.contains('button', 'Filtrer').click()
    cy.wait('@getAuditByEntity')
  })

  it('enforces frontend role guards for admin routes', () => {
    stubUsersApi()
    stubSettingsAndAuditApi()

    visitAsRole('/admin/brukere', 'MANAGER')
    cy.url().should('include', '/forbidden')

    visitAsRole('/admin/innstillinger', 'MANAGER')
    cy.url().should('include', '/admin/innstillinger')

    visitAsRole('/admin/innstillinger', 'EMPLOYEE')
    cy.url().should('include', '/forbidden')
  })

  it('blocks module routes when module is disabled in organization settings', () => {
    cy.intercept('GET', '**/api/v1/organizations/*/settings*', {
      ...settingsPayload,
      enableFoodModule: false,
      enableAlcoholModule: true,
    }).as('getSettingsDisabledFood')

    visitAsRole('/ikmat')
    cy.wait('@getSettingsDisabledFood')
    cy.url().should('include', '/forbidden')
  })

  // ==========================================
  // REAL-TIME VALIDATION TESTS
  // ==========================================
  
  it('shows real-time validation errors for invalid email', () => {
    stubSettingsAndAuditApi()

    visitAsRole('/admin/innstillinger')
    cy.wait('@getSettings')
    cy.wait('@getAudit')

    // Type invalid email - should show error immediately
    cy.get('input#notification_email').clear().type('invalid-email')
    cy.contains('Ugyldig e-postformat').should('be.visible')

    // Fix email - error should disappear
    cy.get('input#notification_email').clear().type('valid@example.com')
    cy.contains('Ugyldig e-postformat').should('not.exist')
  })

  it('shows real-time validation for temperature range', () => {
    stubSettingsAndAuditApi()

    visitAsRole('/admin/innstillinger')
    cy.wait('@getSettings')
    cy.wait('@getAudit')

    // Set min temp higher than max temp
    cy.get('input#default_temp_min_c').clear().type('10')
    cy.get('input#default_temp_max_c').clear().type('5')
    
    // Should show error on both fields
    cy.contains('Min temperatur må være mindre enn eller lik maks temperatur').should('be.visible')
    cy.contains('Maks temperatur må være større enn eller lik min temperatur').should('be.visible')

    // Fix the range
    cy.get('input#default_temp_min_c').clear().type('0')
    cy.get('input#default_temp_max_c').clear().type('4')
    
    // Errors should disappear
    cy.contains('Min temperatur må være mindre enn eller lik maks temperatur').should('not.exist')
    cy.contains('Maks temperatur må være større enn eller lik min temperatur').should('not.exist')
  })

  it('shows validation error when both modules are disabled', () => {
    stubSettingsAndAuditApi()

    visitAsRole('/admin/innstillinger')
    cy.wait('@getSettings')
    cy.wait('@getAudit')

    // Disable both modules
    cy.get('input#enable_food_module').uncheck({ force: true })
    cy.get('input#enable_alcohol_module').uncheck({ force: true })

    // Should show error
    cy.contains('Minst én modul må være aktivert').should('be.visible')

    // Enable one module - error should disappear
    cy.get('input#enable_food_module').check({ force: true })
    cy.contains('Minst én modul må være aktivert').should('not.exist')
  })

  // ==========================================
  // USERS (BRUKERE) COMPREHENSIVE TESTS
  // ==========================================

  it('creates a new user with role assignment', () => {
    stubUsersApi()
    visitAsRole('/admin/brukere')
    cy.wait('@getOrgSettingsForUsers')
    cy.wait('@getUsers')

    // Click add user button
    cy.contains('button', 'Legg til bruker').click()
    
    // Fill in user form
    cy.get('input[name="displayName"]').type('Test Bruker')
    cy.get('input[name="email"]').type('test.bruker@example.com')
    cy.get('input[name="phone"]').type('+47 999 88 777')
    cy.get('select[name="role"]').select('EMPLOYEE')
    
    // Submit form
    cy.contains('button', 'Opprett bruker').click()
    cy.wait('@createUser')
    
    // Verify success message
    cy.contains('Bruker opprettet').should('be.visible')
    
    // Verify new user appears in list
    cy.contains('Test Bruker').should('be.visible')
    cy.contains('test.bruker@example.com').should('be.visible')
  })

  it('edits an existing user', () => {
    stubUsersApi()
    visitAsRole('/admin/brukere')
    cy.wait('@getOrgSettingsForUsers')
    cy.wait('@getUsers')

    // Click edit on first user
    cy.get('[data-testid="edit-user-btn"]').first().click()
    
    // Change display name
    cy.get('input[name="displayName"]').clear().type('Updated Name')
    
    // Submit
    cy.contains('button', 'Lagre endringer').click()
    cy.wait('@updateUser')
    
    // Verify success
    cy.contains('Bruker oppdatert').should('be.visible')
    cy.contains('Updated Name').should('be.visible')
  })

  it('deactivates and reactivates a user', () => {
    stubUsersApi()
    visitAsRole('/admin/brukere')
    cy.wait('@getOrgSettingsForUsers')
    cy.wait('@getUsers')

    // Find active user and deactivate
    cy.contains('Ansatt One')
      .closest('tr')
      .find('[data-testid="deactivate-user-btn"]')
      .click()
    
    // Confirm deactivation
    cy.contains('Bekreft deaktivering').should('be.visible')
    cy.contains('button', 'Deaktiver').click()
    cy.wait('@updateUser')
    
    // Verify user shows as inactive
    cy.contains('Ansatt One')
      .closest('tr')
      .contains('Inaktiv')
      .should('be.visible')
  })

  it('validates required fields when creating user', () => {
    stubUsersApi()
    visitAsRole('/admin/brukere')
    cy.wait('@getOrgSettingsForUsers')
    cy.wait('@getUsers')

    // Click add user
    cy.contains('button', 'Legg til bruker').click()
    
    // Try to submit without filling required fields
    cy.contains('button', 'Opprett bruker').click()
    
    // Should show validation errors
    cy.contains('Navn er påkrevd').should('be.visible')
    cy.contains('E-post er påkrevd').should('be.visible')
    
    // Fill in invalid email
    cy.get('input[name="email"]').type('invalid-email')
    cy.contains('button', 'Opprett bruker').click()
    cy.contains('Ugyldig e-postformat').should('be.visible')
  })

  // ==========================================
  // LOCATIONS (LOKASJONER) TESTS
  // ==========================================

  const locationsPayload = [
    {
      locationId: 1,
      locationName: 'Hovedkjøkken',
      address: 'Storgata 1, 0155 Oslo',
      isActive: true,
      createdAt: '2026-01-01T10:00:00Z',
    },
    {
      locationId: 2,
      locationName: 'Bar',
      address: 'Storgata 1, 0155 Oslo',
      isActive: true,
      createdAt: '2026-01-02T10:00:00Z',
    },
  ]

  const stubLocationsApi = () => {
    cy.intercept('GET', '**/api/v1/organizations/*/locations*', locationsPayload).as('getLocations')
    cy.intercept('POST', '**/api/v1/organizations/*/locations', {
      statusCode: 201,
      body: {
        locationId: 3,
        locationName: 'Nytt Lokale',
        address: 'Nygate 5, 0155 Oslo',
        isActive: true,
        createdAt: '2026-01-03T10:00:00Z',
      },
    }).as('createLocation')
    cy.intercept('PUT', '**/api/v1/organizations/*/locations/*', (req) => {
      req.reply({
        statusCode: 200,
        body: {
          ...req.body,
          locationId: req.url.match(/locations\/(\d+)/)?.[1] || 1,
        },
      })
    }).as('updateLocation')
    cy.intercept('DELETE', '**/api/v1/organizations/*/locations/*', {
      statusCode: 204,
    }).as('deleteLocation')
  }

  it('renders locations page with list', () => {
    stubLocationsApi()
    visitAsRole('/admin/locations')
    cy.wait('@getLocations')

    cy.contains('h1', 'Lokasjoner').should('be.visible')
    cy.contains('Hovedkjøkken').should('be.visible')
    cy.contains('Bar').should('be.visible')
    cy.contains('Storgata 1, 0155 Oslo').should('be.visible')
  })

  it('creates a new location', () => {
    stubLocationsApi()
    visitAsRole('/admin/locations')
    cy.wait('@getLocations')

    // Click add location
    cy.contains('button', 'Legg til lokasjon').click()
    
    // Fill form
    cy.get('input[name="locationName"]').type('Nytt Lokale')
    cy.get('input[name="address"]').type('Nygate 5, 0155 Oslo')
    
    // Submit
    cy.contains('button', 'Opprett lokasjon').click()
    cy.wait('@createLocation')
    
    // Verify success
    cy.contains('Lokasjon opprettet').should('be.visible')
    cy.contains('Nytt Lokale').should('be.visible')
  })

  it('edits an existing location', () => {
    stubLocationsApi()
    visitAsRole('/admin/locations')
    cy.wait('@getLocations')

    // Click edit on first location
    cy.get('[data-testid="edit-location-btn"]').first().click()
    
    // Update name
    cy.get('input[name="locationName"]').clear().type('Oppdatert Kjøkken')
    
    // Submit
    cy.contains('button', 'Lagre endringer').click()
    cy.wait('@updateLocation')
    
    // Verify
    cy.contains('Lokasjon oppdatert').should('be.visible')
    cy.contains('Oppdatert Kjøkken').should('be.visible')
  })

  it('deactivates a location', () => {
    stubLocationsApi()
    visitAsRole('/admin/locations')
    cy.wait('@getLocations')

    // Click deactivate on first location
    cy.get('[data-testid="deactivate-location-btn"]').first().click()
    
    // Confirm
    cy.contains('Bekreft deaktivering').should('be.visible')
    cy.contains('button', 'Deaktiver').click()
    cy.wait('@updateLocation')
    
    // Verify status changed
    cy.contains('Hovedkjøkken')
      .closest('tr')
      .contains('Inaktiv')
      .should('be.visible')
  })

  it('validates required fields when creating location', () => {
    stubLocationsApi()
    visitAsRole('/admin/locations')
    cy.wait('@getLocations')

    // Click add
    cy.contains('button', 'Legg til lokasjon').click()
    
    // Try submit empty form
    cy.contains('button', 'Opprett lokasjon').click()
    
    // Should show errors
    cy.contains('Navn er påkrevd').should('be.visible')
    cy.contains('Adresse er påkrevd').should('be.visible')
  })

  it('searches and filters locations', () => {
    stubLocationsApi()
    visitAsRole('/admin/locations')
    cy.wait('@getLocations')

    // Search for specific location
    cy.get('.table-search').type('Hovedkjøkken')
    cy.contains('Hovedkjøkken').should('be.visible')
    cy.contains('Bar').should('not.exist')

    // Clear search
    cy.get('.table-search').clear()
    cy.contains('Bar').should('be.visible')
  })
})
