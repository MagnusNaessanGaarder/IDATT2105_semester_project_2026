# TODO - Backend Completion Checklist

## Commit: 6b9c315 (INTERMEDIATE)
## Date: April 6, 2026
## Status: PARTIAL - Major features still missing

---

## ❌ CRITICAL - MUST DO (Blocks Grade B)

### 1. User Management API (#49)
**Status:** NOT STARTED
**Files to Create:**
- `backend/src/main/java/com/example/InternalControl/controller/user/UserController.java`
- `backend/src/main/java/com/example/InternalControl/dto/user/UserCreateRequest.java`
- `backend/src/main/java/com/example/InternalControl/dto/user/UserUpdateRequest.java`
- `backend/src/main/java/com/example/InternalControl/dto/user/UserResponse.java`
- `backend/src/main/java/com/example/InternalControl/security/UserSecurity.java`

**Endpoints Needed:**
- GET /api/users?orgNumber={id} - List users
- GET /api/users/{userId} - Get user
- POST /api/users - Create user
- PUT /api/users/{userId} - Update user
- DELETE /api/users/{userId} - Deactivate user

**Time Estimate:** 2-3 hours
**Impact:** HIGH - Frontend needs this for admin features

### 2. Run Tests & Verify Coverage
**Status:** NOT STARTED
**Commands:**
```bash
cd backend
./mvnw clean test -Dcheckstyle.skip=true
./mvnw jacoco:report
```

**Success Criteria:**
- All tests pass (0 failures)
- Coverage >= 50%
- No compilation errors

**Time Estimate:** 30 minutes
**Impact:** CRITICAL - Required for submission

### 3. Start Application & Verify
**Status:** NOT STARTED
**Commands:**
```bash
cd backend
./mvnw spring-boot:run -DskipTests
```

**Verification:**
- Application starts without BeanCreationException
- Swagger UI accessible at http://localhost:8080/swagger-ui.html
- /v3/api-docs returns 200 (not 500)
- No ERROR logs in console

**Time Estimate:** 30 minutes
**Impact:** CRITICAL - Must run for demo

---

## ❌ HIGH PRIORITY - Should Do (Blocks Grade A)

### 4. Role/Permission API (#90)
**Status:** NOT STARTED
**Files to Create:**
- `backend/src/main/java/com/example/InternalControl/controller/user/RoleController.java`
- `backend/src/main/java/com/example/InternalControl/dto/user/RoleResponse.java`

**Endpoints Needed:**
- GET /api/admin/roles - List roles
- GET /api/admin/roles/user/{userId} - Get user roles
- POST /api/admin/roles/user/{userId} - Assign role
- DELETE /api/admin/roles/user/{userId} - Remove role

**Time Estimate:** 1-2 hours
**Impact:** MEDIUM - Admin feature

### 5. Organization Settings API (#53)
**Status:** NOT STARTED
**Files to Create:**
- `backend/src/main/java/com/example/InternalControl/controller/organization/OrganizationSettingsController.java`
- `backend/src/main/java/com/example/InternalControl/dto/organization/OrganizationSettingsRequest.java`
- `backend/src/main/java/com/example/InternalControl/dto/organization/OrganizationSettingsResponse.java`
- `backend/src/main/java/com/example/InternalControl/repository/organization/OrganizationSettingsRepository.java`

**Endpoints Needed:**
- GET /api/admin/organizations/settings - Get settings
- PUT /api/admin/organizations/settings - Update settings

**Time Estimate:** 1-2 hours
**Impact:** MEDIUM - Configuration feature

---

## ❌ MEDIUM PRIORITY - Nice to Have

### 6. Missing Entities
**Status:** NOT STARTED
**Files to Create:**
- `backend/src/main/java/com/example/InternalControl/model/notification/Notification.java`
- `backend/src/main/java/com/example/InternalControl/repository/notification/NotificationRepository.java`
- `backend/src/main/java/com/example/InternalControl/model/audit/AuditLog.java`
- `backend/src/main/java/com/example/InternalControl/repository/audit/AuditLogRepository.java`
- `backend/src/main/java/com/example/InternalControl/model/training/TrainingRecord.java`
- `backend/src/main/java/com/example/InternalControl/repository/training/TrainingRecordRepository.java`

**Time Estimate:** 2-3 hours
**Impact:** LOW - Database schema exists, just needs Java entities

### 7. Frontend Integration
**Status:** NOT STARTED
**Work Needed:**
- Update frontend API services to use new endpoints
- Test end-to-end functionality
- Fix any CORS issues

**Time Estimate:** 2-4 hours
**Impact:** MEDIUM - Full stack functionality

---

## ✅ COMPLETED (in commit 6b9c315)

- [x] Add @PreAuthorize to all 25 endpoints (SECURITY)
- [x] Fix integration test configuration
- [x] Add 24 controller unit tests
- [x] Configure H2 database for testing
- [x] Add H2 dependency to pom.xml

---

## 📊 ESTIMATED TIME REMAINING

**To reach Grade B:** 4-6 hours
- User Management API (2-3h)
- Run tests & fix issues (1h)
- Start app & verify (1h)
- Bug fixes (1-2h)

**To reach Grade A:** 8-10 hours
- Everything for Grade B (4-6h)
- Role/Permission API (1-2h)
- Organization Settings (1-2h)
- Missing entities (2-3h)

---

## 🎯 RECOMMENDED PRIORITY FOR TODAY

**If you have 4 hours:**
1. User Management API (2h)
2. Run tests & fix (1h)
3. Start app & verify (1h)

**If you have 8 hours:**
1. User Management API (2h)
2. Role/Permission API (1h)
3. Organization Settings (1h)
4. Run tests & fix (2h)
5. Start app & verify (2h)

---

## 🚨 BLOCKERS

1. **Tests not run** - Don't know if code compiles or works
2. **App not started** - Don't know if there are bean errors
3. **User Management missing** - Frontend cannot manage users

---

END OF TODO