# Cypress E2E Testing Guide

This guide walks you through running the end-to-end (E2E) test suite for the frontend application. The tests validate role-based authentication, menu access controls, CRUD operations, and session management.

## What's Being Tested

The E2E test suite (`cypress/e2e/role-menu-access.cy.ts`) covers:

- **Authentication**: Login flows, session token validation, logout behavior
- **Role-Based Access Control**: ADMIN, MANAGER, and STAFF permission differences
- **Menu Visibility**: Ensures role-specific menu items appear/hide correctly
- **CRUD Operations**: Create, read, update, delete permissions for different data types (temperature points, daily controls)
- **Menu Navigation**: Verifies all menu routes are accessible and functional
- **Token Refresh**: Validates session token refresh logic
- **Session Management**: Ensures sessionStorage is properly cleaned on logout

## Prerequisites

### Required
- **Node.js** 16+ and npm installed
- **Linux/WSL environment** (tests are configured for Linux; Windows/macOS may require additional setup)
- **Backend API running** at `http://localhost:8080` (the tests make actual API calls)
- **Frontend dev dependencies installed** (`npm install` in `/frontend` directory)

### Optional
- **Chrome/Chromium browser** installed (Cypress will use this; otherwise Electron is bundled)
- **Knowledge of Cypress basics** (helpful but not required; guide assumes no prior experience)

## Quick Start

### Step 1: Install Dependencies

```bash
cd frontend
npm install
```

### Step 2: Ensure Backend is Running

The E2E tests make real API calls to the backend. Start your backend server:
```bash
# In a separate terminal, from the project root
make up  # or your preferred backend startup command
```

The backend should be running at `http://localhost:8080`.

### Step 3: Run the Tests

Choose one of the following based on your need:

#### **Development Mode** (Recommended for Development)
Opens Cypress in an interactive headed browser. Perfect for writing, debugging, and watching tests run live.

```bash
npm run test:e2e:dev
```

**What happens:**
1. Starts the Vite dev server on port 4173
2. Waits for it to be ready
3. Opens Cypress with an interactive UI
4. You can click individual tests to run them, watch execution, and see real-time failures

**Best for:** Debugging, developing new tests, understanding failures

---

#### **Headless Mode** (Recommended for CI/CD)
Runs all tests in headless mode (no browser window). Faster and suitable for automation.

```bash
npm run test:e2e
```

**What happens:**
1. Builds the frontend for production
2. Serves the built app on port 4173
3. Runs all tests in headless Chromium
4. Prints results to terminal and exits

**Best for:** CI/CD pipelines, automated testing, getting quick pass/fail reports

---

#### **Manual Cypress Control**
If you want to run Cypress with custom flags:

```bash
# Open Cypress UI (interactive)
npm run cypress:open:e2e

# Run tests in headless mode with specific options
npm run cypress:run:e2e --spec="cypress/e2e/role-menu-access.cy.ts"

# Run a specific test by name (partial match)
npm run cypress:run:e2e -- --grep="logs in as admin"
```

## Understanding Test Results

### In Interactive Mode (`npm run test:e2e:dev`)

1. Cypress window opens showing all test files and test cases
2. Click a test name on the left panel to run just that test
3. Watch the browser on the right execute each step
4. **Green checkmark** = test passed
5. **Red X** = test failed
6. Click the test name again to re-run it
7. Hover over assertions to see exact values

### In Headless Mode (`npm run test:e2e`)

Terminal output shows:
```
====================================
  (Run Starting)
  
  ├─ Browser: Chromium 90 (headless)
  └─ Spec: role-menu-access.cy.ts
  
  ✓ logs in as admin and stores a valid session token (3s)
  ✓ blocks regular users from create/delete (2s)
  ✓ as admin can navigate menu routes (8s)
  ...
  
  ✓ 8 specs passed in 45s
====================================
```

**Exit code 0** = All tests passed  
**Exit code 1** = One or more tests failed

## Common Issues & Solutions

### Issue: `Backend not reachable at http://localhost:8080`

**Solution:** Make sure your backend is running:
```bash
# In a separate terminal
make up
```

Wait for the backend to fully initialize before running tests. Check `http://localhost:8080/health` in your browser—you should get a response.

---

### Issue: Cypress takes a long time to start (first run only)

**Solution:** On Linux/WSL, Cypress needs to bootstrap graphics libraries. This happens once on first run and takes ~30 seconds. Subsequent runs are fast. This is normal and safe.

---

### Issue: `selectors not found` or `button not visible`

**Solution:** The tests use CSS selectors and aria-labels. If the UI has changed:

1. Run tests in interactive mode: `npm run test:e2e:dev`
2. Click the failing test to highlight where it stopped
3. Open Cypress DevTools (click the browser console icon in the test runner)
4. Inspect the page to find new selector names
5. Update the selector in `cypress/e2e/role-menu-access.cy.ts`

Common selectors in tests:
- `.checklist-card` - Checklist cards in the admin checklist section
- `.control-card` - Control cards in daily controls
- `[aria-label="..."]` - Elements with accessible labels
- `button:contains("Edit")` - Buttons by text content

---

### Issue: `Network requests failing with 403 Forbidden`

**Solution:** The tests use different user roles (ADMIN, MANAGER, STAFF) with different permissions. Make sure:

1. Backend is configured to accept requests from `http://localhost:4173`
2. CORS is enabled in your backend config
3. You're testing against the correct environment (`localhost:8080`, not production)

---

## Writing Your Own Tests

### Test Structure

All tests follow this pattern:

```typescript
describe("Feature name", () => {
  it("should do something", () => {
    // 1. Setup (login, navigate to page, etc.)
    cy.login("admin@test.com", "password")
    cy.visit("/dashboard")
    
    // 2. Action (click, type, submit, etc.)
    cy.get("button:contains('Create')").click()
    cy.get("input[name='name']").type("New Item")
    cy.get("form").submit()
    
    // 3. Assert (verify expected outcome)
    cy.contains("Item created successfully").should("be.visible")
    cy.get(".item-list").should("contain", "New Item")
  })
})
```

### Available Custom Commands

The following custom commands are pre-configured and available:

#### `cy.login(email, password)`
Logs in with the given credentials and validates session token.

```typescript
cy.login("admin@test.com", "password")
// After: sessionStorage contains valid JWT, orgNumber is set
```

#### `cy.clearSession()`
Clears the session (logs out the user).

```typescript
cy.clearSession()
// After: sessionStorage is empty, user is logged out
```

### Common Assertions

```typescript
// Visibility
cy.get(".element").should("be.visible")
cy.get(".element").should("not.exist")

// Text content
cy.contains("Hello World").should("exist")

// Form inputs
cy.get("input[name='email']").should("have.value", "test@example.com")

// API responses (intercept requests)
cy.intercept("POST", "/api/temp-points").as("createTemp")
// ... action that triggers the request ...
cy.wait("@createTemp").its("response.statusCode").should("equal", 201)
```

### Example: Adding a New Test

1. Open `frontend/cypress/e2e/role-menu-access.cy.ts`
2. Add a new test case in the `describe` block:

```typescript
it("should validate that managers cannot delete items", () => {
  cy.login("manager@test.com", "password")
  cy.visit("/dashboard")
  
  // Verify delete button is not visible
  cy.get(".item-card").first().should("not.contain", "Delete")
  
  // Try to call API directly—should be forbidden
  cy.request({
    method: "DELETE",
    url: "/api/items/1",
    failOnStatusCode: false
  }).then(response => {
    expect(response.status).to.equal(403)
  })
})
```

3. Save and run in interactive mode: `npm run test:e2e:dev`
4. Your new test appears in the left panel and can be run individually

## Test Configuration Files

### Key Files You Might Need to Modify

| File | Purpose | When to Edit |
|------|---------|--------------|
| `cypress/e2e/role-menu-access.cy.ts` | Main E2E test suite | Add, modify, or remove test cases |
| `cypress/support/commands.ts` | Custom commands (login, etc.) | Add new helper commands |
| `cypress.config.ts` | Cypress settings | Change timeouts, add plugins, configure reporters |
| `cypress/cypress.env.json` | Test environment variables | Store API URLs, test credentials, feature flags |

## Advanced Usage

### Run a Specific Test File

```bash
npm run cypress:run:e2e -- --spec="cypress/e2e/role-menu-access.cy.ts"
```

### Run Tests Matching a Pattern

```bash
npm run cypress:run:e2e -- --grep="admin"
```

### Generate a Test Report

```bash
npm run cypress:run:e2e -- --reporter=json --reporter-options=reportDir=cypress/reports
```

### Update Snapshots (if using visual regression)

```bash
npm run cypress:run:e2e -- --update-snapshots
```

### Run in Different Browser

```bash
npm run cypress:run:e2e -- --browser=firefox
```

## Debugging Tests

### Method 1: Interactive Debugging (Easiest)

```bash
npm run test:e2e:dev
```

Click on a test, then use Cypress's built-in tools:
- **Step through**: Click the step in the command log to jump to that point
- **Pause**: Click the pause button to stop execution
- **Inspect elements**: Right-click on highlighted elements in the browser
- **Check console**: Open browser DevTools (F12) to see JavaScript logs

### Method 2: Debug via Node Inspector

```bash
npm run cypress:run:e2e -- --inspect
```

Opens a Node.js debugger. Requires additional setup with IDE.

### Method 3: Print Debugging

Add `cy.log()` or `cy.debug()` in your tests:

```typescript
cy.get(".element").then(el => {
  cy.log("Found element:", el.text())
  cy.debug() // Pauses execution here
})
```

### Method 4: Wait and Examine

Use `cy.pause()` to pause execution at any point:

```typescript
it("should do something", () => {
  cy.login("admin@test.com", "password")
  cy.pause() // Execution stops here; you can now interact with the page manually
  cy.get(".element").should("exist")
})
```

## Linux/WSL Library Bootstrap (Behind the Scenes)

**You don't need to do anything—this is automatic.**

On Linux/WSL systems, Cypress requires graphics libraries that may not be installed system-wide. The wrapper script (`scripts/cypress-with-libs.sh`) handles this automatically:

- **First run**: Downloads and extracts required libraries to `~/.local/cypress-libs/` (takes ~30 seconds)
- **Subsequent runs**: Reuses cached libraries (instant startup)
- **No sudo required**: Everything happens in your user home directory
- **No system modifications**: Safe to run on any Linux/WSL system

If you ever need to clear the cache and re-bootstrap:

```bash
rm -rf ~/.local/cypress-libs
npm run test:e2e:dev  # Will re-bootstrap on next run
```

## Additional Resources

- **Cypress Official Docs**: https://docs.cypress.io/
- **Test File Location**: `frontend/cypress/e2e/role-menu-access.cy.ts`
- **Custom Commands**: `frontend/cypress/support/commands.ts`
- **Backend API Docs**: See `docs/BACKEND_GUIDE.md`
- **Frontend Setup**: See `frontend/FRONTEND_DEVELOPER_GUIDE.md`

## Quick Reference: npm Commands

```bash
# Start tests in interactive mode (recommended for development)
npm run test:e2e:dev

# Run all tests in headless mode (for CI/CD)
npm run test:e2e

# Open Cypress UI directly
npm run cypress:open:e2e

# Run Cypress in headless mode with custom options
npm run cypress:run:e2e

# Show Cypress version and verify setup
npm run cypress:run:e2e -- --version
```

## Support & Troubleshooting

If you encounter issues:

1. **Check the terminal output** for specific error messages
2. **Run in interactive mode** to see exactly where tests fail
3. **Verify backend is running** and accessible
4. **Check Cypress logs**: Run `npm run cypress:run:e2e -- --verbose` for detailed logging
5. **Consult Cypress docs**: https://docs.cypress.io/

---

**Last Updated:** April 2026  
**Maintained By:** Development Team  
**Related Files:** 
- Frontend guide: `docs/FRONTEND_GUIDE.md`
- Backend guide: `docs/BACKEND_GUIDE.md`
- Test suite: `frontend/cypress/e2e/role-menu-access.cy.ts`
