# Backend Developer Guide - IDATT2105

**Project:** Internal Control System (IK-Kontroll)  
**Stack:** Spring Boot 3.x, Java 21, MySQL, Flyway  
**Architecture:** Layered with Feature-based organization  
**Last Updated:** 2026-04-02

---

## 1. Architecture Overview

### 1.1 Layered Architecture

```
HTTP Request
    ↓
Controller (REST API)
    ↓
Service (Business Logic)
    ↓
Repository (Data Access)
    ↓
Database (MySQL)
```

**Rules:**
- Controller calls Service only
- Service calls Repository only  
- Repository calls Database only
- Data flows via DTOs between layers

### 1.2 Feature-Based Organization

```
com.example.InternalControl/
├── controller/
│   ├── auth/
│   ├── checklist/
│   ├── deviation/
│   ├── document/
│   └── location/
├── service/
│   ├── auth/
│   ├── checklist/mapper/
│   ├── deviation/mapper/
│   └── ...
├── repository/
│   ├── auth/
│   ├── checklist/
│   └── ...
├── model/
│   ├── auth/
│   ├── checklist/
│   └── ...
└── dto/
    ├── auth/
    ├── checklist/request/
    ├── checklist/response/
    └── ...
```

**Benefits:**
- Related code grouped together
- Easy to find, modify, delete features
- Maximum 6-8 files per package

---

## 2. Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Language |
| Spring Boot | 3.4.x | Framework |
| Spring Security | 6.x | Authentication |
| Spring Data JPA | 3.x | Database access |
| MySQL | 8.x | Database |
| Flyway | 10.x | Migrations |
| JWT | 0.12.x | Tokens |
| Lombok | 1.18.x | Boilerplate |
| Maven | 3.9.x | Build |

---

## 3. Code Standards

### 3.1 Naming Conventions

**Classes:**
- `ChecklistTemplateController` - REST controller
- `ChecklistTemplateService` - Service interface
- `ChecklistTemplateServiceImpl` - Service implementation
- `ChecklistTemplateRepository` - Data access
- `ChecklistTemplateRequest` - Request DTO
- `ChecklistTemplateResponse` - Response DTO

**Methods:**
- `createTemplate()` - Create operation
- `findById()` - Read by ID (not `getById()`)
- `updateTemplate()` - Update operation
- `deleteTemplate()` - Delete operation
- `isValid()` - Boolean check

**Variables:**
- camelCase: `checklistTemplate`
- Constants: `BEARER_PREFIX_LENGTH`

### 3.2 Annotations

**Always use:**
```java
@RequiredArgsConstructor  // Constructor injection
@RestController           // REST endpoints
@Service                  // Business logic
@Repository              // Data access
```

**Never use:**
```java
@Autowired               // Field injection (old way)
@Getter @Setter          // On same line (hard to read)
```

### 3.3 JavaDoc

Required on all public methods:
```java
/**
 * Creates a new checklist template.
 *
 * @param request the creation request
 * @param orgNumber the organization number
 * @param userId the user ID
 * @return the created template as DTO
 * @throws EntityNotFoundException if organization not found
 */
public ChecklistTemplateResponse createTemplate(
    ChecklistTemplateCreateRequest request,
    Integer orgNumber, 
    Long userId)
```

---

## 4. Layer Responsibilities

### 4.1 Controller Layer

**Allowed:**
- Receive HTTP requests
- Validate input with `@Valid`
- Call service layer
- Return ResponseEntity with DTOs

**Not Allowed:**
- Business logic
- Database queries
- Direct repository access

```java
@RestController
@RequestMapping("/api/checklists/templates")
@RequiredArgsConstructor
public class ChecklistTemplateController {
    private final ChecklistTemplateService templateService;
    private final ChecklistTemplateMapper templateMapper;

    @GetMapping
    public ResponseEntity<List<ChecklistTemplateResponse>> getTemplates(
            @RequestParam Integer orgNumber) {
        return ResponseEntity.ok(
            templateService.findByOrg(orgNumber).stream()
                .map(templateMapper::toResponse)
                .collect(Collectors.toList())
        );
    }
}
```

### 4.2 Service Layer

**Allowed:**
- Business logic
- Orchestrate repositories
- Transaction management with `@Transactional`
- Input validation

**Not Allowed:**
- HTTP requests/responses
- Direct SQL (use repositories)

```java
@Service
@RequiredArgsConstructor
public class ChecklistTemplateServiceImpl implements ChecklistTemplateService {
    private final ChecklistTemplateRepository repository;
    
    @Override
    @Transactional
    public ChecklistTemplate createTemplate(ChecklistTemplate template, 
                                           Integer orgNumber, 
                                           Long userId) {
        // Business logic here
        return repository.save(template);
    }
}
```

### 4.3 Repository Layer

**Allowed:**
- Database queries
- CRUD operations
- Custom queries with @Query

**Not Allowed:**
- Business logic
- HTTP handling

```java
@Repository
public interface ChecklistTemplateRepository 
    extends JpaRepository<ChecklistTemplate, Long> {
    
    List<ChecklistTemplate> findByOrgNumber(Integer orgNumber);
}
```

---

## 5. Security

### 5.1 JWT Authentication

**Token Flow:**
1. Client sends credentials to `/api/auth/login`
2. Server validates and returns JWT + refresh token
3. Client stores JWT in sessionStorage
4. Client sends JWT in `Authorization: Bearer <token>` header
5. `JwtAuthenticationFilter` validates token
6. `SecurityContext` is populated with user info

**Key Classes:**
- `JwtService` - Token generation/validation
- `JwtAuthenticationFilter` - Token validation filter
- `SecurityConfig` - Security configuration
- `AuthenticationFacade` - Centralized auth logic

### 5.2 Multi-Tenancy

**Critical:** Every query must include `org_number` scoping from JWT.

```java
// CORRECT - Scope by org_number from JWT
@Query("SELECT d FROM DeviationReport d WHERE d.orgNumber = :orgNumber")
List<DeviationReport> findByOrgNumber(@Param("orgNumber") Integer orgNumber);

// INCORRECT - Never trust user input for org_number
@Query("SELECT d FROM DeviationReport d")  // Missing org_number!
```

### 5.3 Role-Based Access

```java
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) { }

@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public ResponseEntity<?> updateTemplate(@PathVariable Long id, ...) { }
```

### 5.4 OWASP Compliance

| # | Vulnerability | Protection |
|---|---------------|------------|
| A1 | Injection | Parameterized queries |
| A2 | Broken Auth | JWT with expiry, BCrypt |
| A3 | XSS | DTOs, no raw HTML |
| A4 | Insecure Direct Object | org_number scoping |
| A5 | Security Misconfig | Profiles, exception handler |
| A6 | Sensitive Data Exposure | BCrypt, SHA-256 |
| A7 | Missing Access Control | @PreAuthorize on all endpoints |
| A8 | CSRF | JWT in sessionStorage |

---

## 6. DTOs and Mapping

### 6.1 DTO Structure

**Request DTOs:** Incoming data from client
```java
@Data
@Builder
public class ChecklistTemplateCreateRequest {
    @NotBlank
    private String title;
    
    @NotNull
    private ModuleType moduleType;
    
    private List<ChecklistTemplateItemRequest> items;
}
```

**Response DTOs:** Outgoing data to client
```java
@Data
@Builder
public class ChecklistTemplateResponse {
    private Long templateId;
    private String title;
    private ModuleType moduleType;
    private List<ChecklistTemplateItemResponse> items;
    private UserDto createdBy;
    private LocalDateTime createdAt;
}
```

### 6.2 Mapper Pattern

```java
@Component
@RequiredArgsConstructor
public class ChecklistTemplateMapper {
    private final UserMapper userMapper;
    
    public ChecklistTemplateResponse toResponse(ChecklistTemplate template) {
        return ChecklistTemplateResponse.builder()
            .templateId(template.getTemplateId())
            .title(template.getTitle())
            .moduleType(template.getModuleType())
            .items(template.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList()))
            .build();
    }
}
```

**Rules:**
- Never expose entities directly in API
- Map entities to DTOs in service/controller layer
- Use mappers for complex transformations

---

## 7. Testing

### 7.1 Test Structure (AAA)

```java
@Test
void createTemplate_WithValidRequest_ReturnsCreated() {
    // Arrange
    Long userId = 1L;
    Integer orgNumber = 123456789;
    ChecklistTemplateCreateRequest request = createTestRequest();
    
    when(authFacade.extractAndValidateUser(any(), eq(orgNumber)))
        .thenReturn(userId);
    when(templateService.create(any(), eq(orgNumber), eq(userId)))
        .thenReturn(createTestTemplate());
    
    // Act
    ResponseEntity<ChecklistTemplateResponse> response = 
        controller.createTemplate(request, orgNumber, httpRequest);
    
    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getTitle()).isEqualTo("Test Template");
}
```

### 7.2 Test Coverage Requirements

**Minimum for course:**
- Controllers: 100% coverage
- Services: 70% coverage
- Critical business logic: 100% coverage

**Current Status:**
- 32 controller tests (100%)
- 70+ service tests (70%+)
- Total: 112+ backend tests

---

## 8. Database

### 8.1 Migrations (Flyway)

Naming convention:
```
V1__Create_organization_table.sql
V2__Create_app_user_table.sql
V3__Create_checklist_tables.sql
```

**Rules:**
- One migration per feature/table
- Never modify existing migrations
- Use `R__` for repeatable migrations
- Test migrations on H2 before committing

### 8.2 Entity Design

```java
@Entity
@Table(name = "checklist_template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistTemplate extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long templateId;
    
    @Column(nullable = false)
    private String title;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModuleType moduleType;
    
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL)
    private List<ChecklistTemplateItem> items = new ArrayList<>();
}
```

---

## 9. Error Handling

### 9.1 Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage(), LocalDateTime.now()));
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("Access denied", LocalDateTime.now()));
    }
}
```

### 9.2 HTTP Status Codes

| Code | Use Case | Example |
|------|----------|---------|
| 200 | Success | GET operations |
| 201 | Created | POST operations |
| 204 | No Content | DELETE operations |
| 400 | Bad Request | Validation errors |
| 401 | Unauthorized | Missing/invalid JWT |
| 403 | Forbidden | No permission |
| 404 | Not Found | Resource doesn't exist |
| 500 | Server Error | Unexpected errors |

---

## 10. Checklist for New Features

When adding a new feature (e.g., "Temperature"):

**Backend:**
- [ ] Create entity in `model/temperature/`
- [ ] Create repository in `repository/temperature/`
- [ ] Create service interface + impl in `service/temperature/`
- [ ] Create DTOs in `dto/temperature/request/` and `dto/temperature/response/`
- [ ] Create mapper in `service/temperature/mapper/`
- [ ] Create controller in `controller/temperature/`
- [ ] Add Flyway migration
- [ ] Write unit tests
- [ ] Add Swagger annotations
- [ ] Add `@PreAuthorize` if needed

**Frontend:**
- [ ] Create feature folder in `features/temperature/`
- [ ] Create API service in `features/temperature/api.ts`
- [ ] Create composable in `features/temperature/composables/`
- [ ] Create components in `features/temperature/components/`
- [ ] Create views in `features/temperature/views/`
- [ ] Add routes in router
- [ ] Add Pinia store if global state needed
- [ ] Write unit tests

---

## 11. Common Pitfalls

**Don't:**
- Return entities from controllers (always use DTOs)
- Use field injection (`@Autowired` on fields)
- Put business logic in controllers
- Trust user input for org_number
- Skip JavaDoc on public methods
- Use magic numbers (define constants)
- Create God classes (>300 lines)

**Do:**
- Use constructor injection
- Return DTOs from services
- Scope all queries by org_number
- Write tests for critical paths
- Use feature-based organization
- Follow naming conventions
- Keep methods small (<30 lines)

---

## 12. Quick Reference

**Run tests:**
```bash
./mvnw test
./mvnw test -Dtest=ChecklistTemplateControllerTest
```

**Build:**
```bash
./mvnw clean package -DskipTests
```

**Check coverage:**
```bash
./mvnw test jacoco:report
# Open target/site/jacoco/index.html
```

**Run with profile:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

**For questions or issues:**
1. Check this guide
2. Look at existing features (checklist is best example)
3. Ask team lead

**Remember:** Quality over quantity. A working, well-tested feature beats 3 half-finished ones.
