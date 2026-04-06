# JWT Autentiseringsguide - Full Stack

> Komplett implementasjonsguide for JWT-basert autentisering i IDATT2105-prosjektet

---

## Innholdsfortegnelse

1. [Oversikt](#oversikt)
2. [Arkitektur](#arkitektur)
3. [Backend-Implementasjon](#backend-implementasjon)
4. [Frontend-Implementasjon](#frontend-implementasjon)
5. [Sikkerhetsbeste-Praksis](#sikkerhetsbeste-praksis)
6. [Testing](#testing)
7. [Vanlige-Problemer](#vanlige-problemer)

---

## Oversikt

### Hva er JWT?

**JWT (JSON Web Token)** er en kompakt, URL-sikker måte å overføre informasjon mellom to parter på som et JSON-objekt.

**Struktur:**
```
header.payload.signature
```

**Eksempel:**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJ0ZXN0QGV4YW1wbGUuY29tIiwicm9sZSI6IkFETUlOIiwiaWF0IjoxNjk5NTU0ODAwLCJleHAiOjE2OTk2NDEyMDB9.signature
```

### Fordeler med JWT

- **Stateless**: Server trenger ikke lagre session-data
- **Skalerbart**: Enkelt å legge til flere servere
- **Selv-inneholdt**: All info i tokenet
- **Sikkert**: Signert og verifiserbart

---

## Arkitektur

### Dataflyt

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Frontend  │ ──────> │   Backend    │ ──────> │  Database   │
│  (Vue.js)   │         │(Spring Boot) │         │  (MySQL)    │
└─────────────┘         └──────────────┘         └─────────────┘
       │                       │
       │ 1. POST /auth/login   │
       │ {email, password}     │
       │<──────────────────────│
       │ {token, user}         │
       │                       │
       │ 2. GET /api/data      │
       │ Authorization: Bearer │
       │<──────────────────────│
       │ {data}                │
```

### Komponenter

1. **Frontend**: Vue.js med Pinia store
2. **Backend**: Spring Boot med Spring Security
3. **Database**: MySQL med bruker-tabell
4. **Token**: JWT med HS256-algoritme

---

## Backend-Implementasjon

### 1. Avhengigheter (pom.xml)

```xml
<dependencies>
    <!-- Spring Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.3</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

### 2. Konfigurasjon (application.properties)

```properties
# JWT Configuration
jwt.secret=your-256-bit-secret-key-here-at-least-32-characters
jwt.expiration=86400000

# CORS (for frontend)
cors.allowed-origins=http://localhost:5173,http://localhost:3000
```

### 3. Entiteter

**User.java:**
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Getters, setters, constructors
}

enum Role {
    ADMIN, MANAGER, EMPLOYEE
}
```

### 4. JWT Service

**JwtService.java:**
```java
@Service
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    private final SecretKey key;
    
    public JwtService() {
        // Generer nøkkel fra secret
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
    
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.getId().toString())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
        return claims.getSubject();
    }
}
```

### 5. Security Configuration

**SecurityConfig.java:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthFilter;
    
    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/manager/**").hasAnyRole("ADMIN", "MANAGER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### 6. JWT Filter

**JwtAuthenticationFilter.java:**
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final UserRepository userRepository;
    
    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        final String jwt = authHeader.substring(7);
        
        if (!jwtService.validateToken(jwt)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        final String userId = jwtService.getUserIdFromToken(jwt);
        
        User user = userRepository.findById(Long.parseLong(userId))
            .orElse(null);
        
        if (user != null) {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                user, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### 7. Controller

**AuthController.java:**
```java
@RestController
@RequestMapping("/auth")
public class AuthController {
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Ugyldig e-post eller passord"));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // JWT er stateless - logout håndteres client-side
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new UserDTO(user));
    }
}

// DTOs
class LoginRequest {
    @NotBlank
    private String email;
    
    @NotBlank
    private String password;
    
    // Getters, setters
}

class AuthResponse {
    private String token;
    private UserDTO user;
    
    // Constructor, getters
}

class UserDTO {
    private Long id;
    private String email;
    private String name;
    private String role;
    
    public UserDTO(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.role = user.getRole().name();
    }
    
    // Getters
}
```

### 8. Service

**AuthService.java:**
```java
@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    public AuthService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder,
                      JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }
    
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, new UserDTO(user));
    }
}
```

### 9. Database Migration (Flyway)

**V1__Create_users_table.sql:**
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'MANAGER', 'EMPLOYEE') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Test users (password: Test1234!)
INSERT INTO users (email, password, name, role) VALUES 
('kari@everest-sushi.no', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqmXzN6J.0L0W1R3sL7G8Z0K1N9M0', 'Kari Olsen', 'ADMIN'),
('amir@everest-sushi.no', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqmXzN6J.0L0W1R3sL7G8Z0K1N9M0', 'Amir Patel', 'MANAGER'),
('lina@everest-sushi.no', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqmXzN6J.0L0W1R3sL7G8Z0K1N9M0', 'Lina Nguyen', 'EMPLOYEE');
```

---

## Frontend-Implementasjon

### 1. Axios Client med Interceptors

**api/client.ts:**
```typescript
import axios from 'axios'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor - legg til JWT token
client.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('jwt_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor - håndter 401
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token ugyldig eller utløpt
      sessionStorage.removeItem('jwt_token')
      sessionStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default client
```

### 2. Auth API

**features/auth/api.ts:**
```typescript
import client from '@/api/client'
import type { User } from '@/types'

interface LoginCredentials {
  email: string
  password: string
}

interface AuthResponse {
  token: string
  user: User
}

export const authApi = {
  async login(credentials: LoginCredentials): Promise<AuthResponse> {
    const response = await client.post('/auth/login', credentials)
    return response.data
  },

  async logout(): Promise<void> {
    await client.post('/auth/logout')
  },

  async me(): Promise<User> {
    const response = await client.get('/auth/me')
    return response.data
  },
}
```

### 3. Auth Store (Pinia)

**stores/auth.ts:**
```typescript
import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/features/auth/api'
import type { User } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref<User | null>(null)
  const token = ref<string | null>(sessionStorage.getItem('jwt_token'))
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  // Getters
  const isAuthenticated = computed(() => !!token.value && !!user.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isManager = computed(() => ['ADMIN', 'MANAGER'].includes(user.value?.role || ''))

  // Actions
  async function login(credentials: { email: string; password: string }) {
    isLoading.value = true
    error.value = null
    
    try {
      const response = await authApi.login(credentials)
      
      token.value = response.token
      user.value = response.user
      
      // Lagre i sessionStorage
      sessionStorage.setItem('jwt_token', response.token)
      sessionStorage.setItem('user', JSON.stringify(response.user))
      
      return true
    } catch (e: any) {
      error.value = e.response?.data?.message || 'Innlogging feilet'
      return false
    } finally {
      isLoading.value = false
    }
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch (e) {
      // Ignorer feil ved logout
    }
    
    // Clear state
    user.value = null
    token.value = null
    sessionStorage.removeItem('jwt_token')
    sessionStorage.removeItem('user')
  }

  async function checkAuth() {
    const storedToken = sessionStorage.getItem('jwt_token')
    const storedUser = sessionStorage.getItem('user')
    
    if (storedToken && storedUser) {
      try {
        // Verifiser token med backend
        const userData = await authApi.me()
        user.value = userData
        token.value = storedToken
        return true
      } catch (e) {
        // Token ugyldig
        logout()
        return false
      }
    }
    return false
  }

  function hasRole(...roles: string[]) {
    return roles.includes(user.value?.role || '')
  }

  return {
    user,
    token,
    isLoading,
    error,
    isAuthenticated,
    isAdmin,
    isManager,
    login,
    logout,
    checkAuth,
    hasRole,
  }
})
```

### 4. Vue Router Guards

**router/index.ts:**
```typescript
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/features/auth/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/AppShell.vue'),
      meta: { requiresAuth: true },
      children: [
        // Protected routes
      ],
    },
  ],
})

router.beforeEach(async (to, from) => {
  const authStore = useAuthStore()
  
  // Sjekk om bruker er autentisert
  if (to.meta.requiresAuth) {
    if (!authStore.isAuthenticated) {
      const isValid = await authStore.checkAuth()
      if (!isValid) {
        return { name: 'Login', query: { redirect: to.fullPath } }
      }
    }
    
    // Sjekk roller
    if (to.meta.allowedRoles && !authStore.hasRole(...to.meta.allowedRoles)) {
      return { name: 'Dashboard' }
    }
  }
  
  // Redirect fra login hvis allerede innlogget
  if (to.meta.public && authStore.isAuthenticated) {
    return { name: 'Dashboard' }
  }
})

export default router
```

### 5. Login View

**features/auth/views/LoginView.vue:**
```vue
<template>
  <div class="login-page">
    <form @submit.prevent="handleLogin" class="login-form">
      <h2>Logg inn</h2>
      
      <div v-if="error" class="error-message" role="alert">
        {{ error }}
      </div>
      
      <div class="form-group">
        <label for="email">E-post</label>
        <input
          id="email"
          v-model="email"
          type="email"
          required
          autocomplete="email"
          :disabled="isLoading"
        />
      </div>
      
      <div class="form-group">
        <label for="password">Passord</label>
        <input
          id="password"
          v-model="password"
          type="password"
          required
          autocomplete="current-password"
          :disabled="isLoading"
        />
      </div>
      
      <button type="submit" :disabled="isLoading" class="login-btn">
        <span v-if="isLoading">Logger inn...</span>
        <span v-else>Logg inn</span>
      </button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')

const isLoading = computed(() => authStore.isLoading)
const error = computed(() => authStore.error)

async function handleLogin() {
  const success = await authStore.login({
    email: email.value,
    password: password.value,
  })
  
  if (success) {
    const redirect = route.query.redirect as string
    router.push(redirect || { name: 'Dashboard' })
  }
}
</script>
```

---

## Sikkerhetsbeste-Praksis

### 1. Passord-Sikkerhet

```java
// Bruk BCrypt med salt
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12); // 12 rounds
}

// Hash passord før lagring
user.setPassword(passwordEncoder.encode(plainPassword));
```

### 2. Token-Konfigurasjon

```properties
# Kort levetid (24 timer)
jwt.expiration=86400000

# Sterk secret (minst 256 bits)
jwt.secret=minst-32-tegn-lang-og-tilfeldig-secret-key-her
```

### 3. CORS-Konfigurasjon

```java
// Tillat kun frontend
configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
configuration.setAllowCredentials(true);
```

### 4. HTTPS i Produksjon

```properties
# Force HTTPS
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=password
server.ssl.key-store-type=PKCS12
```

### 5. Input-Validering

```java
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {
    // @Valid validerer automatisk
}

class LoginRequest {
    @NotBlank
    @Email
    private String email;
    
    @NotBlank
    @Size(min = 8)
    private String password;
}
```

---

## Testing

### 1. Backend-Tester

**AuthControllerTest.java:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }
    
    @Test
    void login_withInvalidCredentials_returns401() throws Exception {
        mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"wrong@example.com\",\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized());
    }
}
```

### 2. Frontend-Tester

**stores/auth.test.ts:**
```typescript
import { describe, it, expect, vi } from 'vitest'
import { useAuthStore } from './auth'
import { authApi } from '@/features/auth/api'

vi.mock('@/features/auth/api')

describe('Auth Store', () => {
  it('logs in successfully', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      token: 'test-token',
      user: { id: '1', email: 'test@example.com', name: 'Test', role: 'ADMIN' }
    })
    
    const store = useAuthStore()
    const result = await store.login({ email: 'test@example.com', password: 'password' })
    
    expect(result).toBe(true)
    expect(store.isAuthenticated).toBe(true)
    expect(store.user?.email).toBe('test@example.com')
  })
})
```

---

## Vanlige-Problemer

### 1. "Token expired"

**Løsning:** Implementer token refresh
```typescript
// I Axios interceptor
if (error.response?.status === 401 && !originalRequest._retry) {
  originalRequest._retry = true
  await refreshToken()
  return client(originalRequest)
}
```

### 2. "CORS error"

**Løsning:** Sjekk CORS-konfigurasjon
```java
// Backend må tillitte frontend
configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
```

### 3. "Password not matching"

**Løsning:** Sjekk at passord hashes likt
```java
// Ved registrering
String hashed = passwordEncoder.encode(password);

// Ved login
passwordEncoder.matches(inputPassword, storedHash);
```

### 4. "Role not working"

**Løsning:** Sjekk at roller har "ROLE_" prefix
```java
new SimpleGrantedAuthority("ROLE_" + user.getRole())
```

---

## Oppsummering

### Sjekkliste før leveranse

- [ ] JWT secret er minst 32 tegn og tilfeldig
- [ ] Passord hashes med BCrypt (10+ rounds)
- [ ] CORS konfigurert for frontend
- [ ] Token expiry satt (24 timer anbefalt)
- [ ] 401/403 håndteres i frontend
- [ ] Login form har validering
- [ ] Logout clearer sessionStorage
- [ ] Tester for auth flow
- [ ] HTTPS i produksjon
- [ ] Input-sanitering på backend

### Viktig å huske

1. **Stateless**: JWT krever ingen session på server
2. **Client-side storage**: sessionStorage (ikke localStorage)
3. **Secret key**: Hold hemmelig, ikke commit til git
4. **Expiry**: Korte tokens er sikrere
5. **HTTPS**: Alltid i produksjon

---

**Sist oppdatert:** 26. mars 2026  
**For:** IDATT2105 - IK-Kontroll Frontend  
**Forfatter:** Tri
