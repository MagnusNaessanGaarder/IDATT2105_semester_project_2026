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
})
