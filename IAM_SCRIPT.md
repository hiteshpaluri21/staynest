# StayNest — IAM Service Presentation Script (with Code Walkthrough)

> **Scope:** The complete Identity & Access Management story — backend service (entities, security config, JWT, controllers, services, seeding) **and** the frontend (App.jsx routing, AuthContext, ProtectedRoute, login/register, the User Management module).
> **Style:** Explain the code as you show it. Each section has **[SAY]** narration and the **exact code** to display.
> **Suggested length:** 12–15 min. Trim the "Talking points" bullets if short on time.

---

## Part 0 — Framing (30 sec)

**[SAY]**
> "IAM — Identity and Access Management — is the security backbone of StayNest. It's the service that answers two questions for the entire platform: *who are you?* (authentication) and *what are you allowed to do?* (authorization). It issues a JWT on login, and every other microservice trusts that token. I'll walk through it end to end — backend first, then how the React frontend consumes it."

**The one-line mental model to put on screen:**
`Login → IAM issues JWT → browser stores it → every request carries it → each service validates it → role decides access.`

---

# PART 1 — BACKEND (IAM Service, port 8081)

## 1.1 Entry Point & Configuration (1 min)

**File:** `backend/iam-service/src/main/java/com/staynest/iam/IamServiceApplication.java`

```java
@SpringBootApplication
@EnableDiscoveryClient
public class IamServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IamServiceApplication.class, args);
    }
}
```

**[SAY]**
> "Standard Spring Boot bootstrap. The important annotation is `@EnableDiscoveryClient` — it registers IAM with the **Eureka** service registry, so the other services can find it by name instead of a hardcoded URL."

**File:** `backend/iam-service/src/main/resources/application.yml`

**[SAY]** — point to these values:
> "Port **8081**. Its own database, **iam_db** — remember, database-per-service. And the two JWT settings that matter: a signing **secret**, and an **expiration of 86,400,000 ms — exactly 24 hours**."

```yaml
server:
  port: 8081
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/iam_db
jwt:
  secret: staynest-secret-key-for-jwt-signing-2026
  expiration: 86400000   # 24 hours
```

**Talking point (honesty slide):** "The secret and DB password are in plaintext here — fine for a demo; in production these move to environment variables or a secrets vault."

---

## 1.2 The Data Model — Entities & Enums (1.5 min)

**File:** `.../iam/entity/User.java`

**[SAY]**
> "The `User` entity is the core record. Lombok's `@Data`/`@Builder` generate the boilerplate. Three fields carry the security weight."

```java
@Entity
@Table(name = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @Enumerated(EnumType.STRING)          // stored as "ADMIN", not 0/1/2
    private Role role;

    @Column(unique = true, nullable = false)
    private String email;                 // login identity — must be unique

    private String password;              // stores the BCrypt HASH, never plaintext

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;   // defaults to ACTIVE

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp   private LocalDateTime updatedAt;
}
```

**Talking points:**
- `@Enumerated(EnumType.STRING)` — roles are stored as readable strings, so the DB is self-documenting and reordering the enum can't corrupt data.
- `email` is **unique** — it's the login handle.
- `password` holds a **BCrypt hash**, set only through the service layer — plaintext never touches the DB.
- No JPA relationships — role is just an enum column, keeping the service lightweight.

**File:** `.../iam/enums/Role.java`

```java
public enum Role { GUEST, FRONTDESK, HOUSEKEEPING, FBMANAGER, REVENUEMANAGER, ADMIN }
```

> "Six roles spanning hotel operations. **GUEST** is the default for public self-registration; **ADMIN** is the seeded superuser who creates all staff accounts."

**File:** `.../iam/enums/UserStatus.java` — `ACTIVE, INACTIVE`. "This drives soft-delete and blocks deactivated accounts from logging in."

**File:** `.../iam/entity/AuditLog.java` — "Every sensitive action writes one of these: who did it (`userId`), what (`action` like `CREATE_USER`), on which record (`entityType` + `entityId`), and when (auto-timestamped). It's the compliance trail."

---

## 1.3 Security Configuration — the heart (3 min)

This is the most important section. Three files: `SecurityConfig`, `JwtUtil`, `JwtFilter`.

### 1.3.1 SecurityConfig — the rules of the road

**File:** `.../iam/config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // turns on @PreAuthorize in controllers
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtFilter jwtFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()   // login + register are public
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated())                 // everything else needs a token
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

**[SAY] — walk these five points:**
1. **`STATELESS` sessions** — "No server session, no cookie. Every single request must prove itself with the JWT. This is what makes the whole architecture horizontally scalable — any instance can serve any request."
2. **CSRF disabled** — "Correct for a token-based API; CSRF protection is only needed for cookie-based sessions."
3. **The permit list** — "Three things are public: CORS pre-flight `OPTIONS`, everything under `/api/auth/**` (login and register — you can't have a token before you log in), and health actuators. **`anyRequest().authenticated()`** locks down everything else."
4. **`@EnableMethodSecurity`** — "This activates the `@PreAuthorize("hasRole('ADMIN')")` annotations we'll see on the controllers — fine-grained, per-endpoint role checks."
5. **`addFilterBefore(jwtFilter, ...)`** — "Our custom JWT filter runs **before** Spring's default username/password filter, so by the time the request reaches a controller, the user's identity and role are already loaded."
- **`passwordEncoder` bean** — "BCrypt: salted, slow-by-design hashing. This is the bean that both hashes on register and verifies on login."

### 1.3.2 JwtUtil — minting and reading tokens

**File:** `.../iam/config/JwtUtil.java`

```java
@Component
public class JwtUtil {
    @Value("${jwt.secret}")     private String secret;
    @Value("${jwt.expiration}") private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)                // WHO: identity
                .claim("role", role)              // WHAT: authority
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))  // +24h
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)   // tamper-proof signature
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;   // bad signature, expired, or malformed
        }
    }

    public String extractEmail(String token) { return getClaims(token).getSubject(); }
    public String extractRole(String token)  { return getClaims(token).get("role", String.class); }
}
```

**[SAY]**
> "A JWT is a signed, self-contained ID card. `generateToken` puts the **email in the subject** and the **role in a custom claim**, stamps it with a 24-hour expiry, and signs it with **HMAC-SHA256** using our secret. Because it's signed, nobody can change the role to ADMIN without invalidating the signature."
> "`validateToken` just tries to parse it — any tampering, expiry, or corruption throws and returns false. The two `extract` methods read identity and role back out. Notice there's **no database call** — the token carries everything, which is exactly why every other service can validate it independently without calling IAM."

### 1.3.3 JwtFilter — enforcing it on every request

**File:** `.../iam/config/JwtFilter.java`

```java
@Component @RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);              // strip "Bearer "
            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.extractEmail(token);
                String role  = jwtUtil.extractRole(token);
                var auth = new UsernamePasswordAuthenticationToken(
                    email, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));  // note the ROLE_ prefix
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
```

**[SAY]**
> "This runs **once per request**. It reads the `Authorization: Bearer ...` header, validates the token, pulls out email and role, and loads them into Spring's `SecurityContext`. The key detail is the **`ROLE_` prefix** — Spring's `hasRole('ADMIN')` actually looks for an authority called `ROLE_ADMIN`, so we prepend it here."
> "If there's no token or it's invalid, the filter just does nothing and moves on — and then `anyRequest().authenticated()` in the config rejects the request with a 401. Fail-closed by default."

### 1.3.4 DataSeeder — the bootstrap admin

**File:** `.../iam/config/DataSeeder.java`

```java
@Component @RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .name("System Admin")
                    .email("admin@staynest.com")
                    .password(passwordEncoder.encode("Admin@123"))   // hashed, not plaintext
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(admin);
        }
    }
}
```

**[SAY]**
> "A chicken-and-egg problem: only an admin can create staff, but who creates the first admin? This `CommandLineRunner` runs at startup and, **only if the users table is empty**, seeds one admin — `admin@staynest.com` / `Admin@123`, password already BCrypt-hashed. It's idempotent, so restarting never creates duplicates. This is the account we'll log in with in the demo."

---

## 1.4 Controllers — the API surface (2.5 min)

### 1.4.1 AuthController — public login & register

**File:** `.../iam/controller/AuthController.java`

**LOGIN:**
```java
@PostMapping("/login")
public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    User user = userRepository.findByEmail(request.getEmail()).orElse(null);

    if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword()))
        return ResponseEntity.status(401).body(ApiResponse.error("Invalid email or password"));

    if (user.getStatus() == UserStatus.INACTIVE)
        return ResponseEntity.status(403).body(ApiResponse.error("Account is deactivated"));

    String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
    LoginResponse resp = LoginResponse.builder()
            .token(token).role(user.getRole()).userId(user.getUserId())
            .email(user.getEmail()).name(user.getName()).build();
    return ResponseEntity.ok(ApiResponse.success(resp));
}
```

**[SAY]**
> "Login does three checks: does the user exist, does `passwordEncoder.matches` confirm the raw password against the stored hash, and is the account still ACTIVE. Note the error message is deliberately vague — *'Invalid email or password'* — so an attacker can't tell whether an email exists. On success it mints the JWT and returns it along with the role, id, and name."

**REGISTER — the security-critical line:**
```java
@PostMapping("/register")
public ResponseEntity<?> register(@Valid @RequestBody UserRequest request) {
    if (userRepository.existsByEmail(request.getEmail()))
        return ResponseEntity.badRequest().body(ApiResponse.error("Email is already registered"));

    request.setRole(Role.GUEST);          // <-- FORCE guest, ignore whatever the client sent
    UserResponse created = userService.createUser(request);
    String token = jwtUtil.generateToken(created.getEmail(), created.getRole().name());
    // ...return 201 with token (auto-login)
}
```

**[SAY] — emphasize this:**
> "This one line — `request.setRole(Role.GUEST)` — is a critical security control. Public registration **forcibly overrides** whatever role the client sends. So even if someone crafts a request with `role: ADMIN`, they get a GUEST. Privileged roles can **only** be assigned by an existing admin through the protected user endpoint. Register then auto-logs-in by returning a token."

### 1.4.2 UserController — admin-guarded management

**File:** `.../iam/controller/UserController.java`

```java
@PostMapping
@PreAuthorize("hasRole('ADMIN')")                       // only admin creates staff (role honored here)
public ResponseEntity<?> create(@Valid @RequestBody UserRequest r) { ... }

@GetMapping
@PreAuthorize("hasAnyRole('ADMIN','REVENUEMANAGER')")   // list users
public ResponseEntity<?> all() { ... }

@PatchMapping("/{id}/status")
@PreAuthorize("hasRole('ADMIN')")                        // activate / deactivate
public ResponseEntity<?> updateStatus(@PathVariable Integer id, @RequestParam UserStatus status,
                                      Authentication auth) {
    if (isSelf(auth, id))
        throw new BadRequestException("You cannot deactivate your own account");
    ...
}

@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")                        // soft delete
public ResponseEntity<?> delete(@PathVariable Integer id, Authentication auth) {
    if (isSelf(auth, id))
        throw new BadRequestException("You cannot delete your own account");
    ...
}
```

**[SAY]**
> "This is where `@PreAuthorize` earns its keep. `hasRole('ADMIN')` gates creating, deactivating, and deleting users — enforced by the backend, not by the UI. Two nice safety rails: an admin **cannot deactivate or delete their own account** — `isSelf()` compares the caller's email from the JWT against the target id, so you can't accidentally lock the last admin out."
> "One deliberate exception: `GET /api/users/role/{role}` has no role restriction — it's open to any authenticated service so sibling microservices can look up, say, all FRONTDESK staff to send them a notification."

**Contrast to highlight:** "Same `role` field, two behaviors — on **public register** it's overridden to GUEST; on **admin create** it's honored. That's the whole privilege model in a nutshell."

---

## 1.5 Service Layer, Repositories, DTOs, Exceptions (1.5 min)

**Service — `UserServiceImpl`:** "This is where the password gets **BCrypt-encoded** before save, and where every mutation writes an audit log."
```java
public UserResponse createUser(UserRequest req) {
    if (userRepository.existsByEmail(req.getEmail()))
        throw new BadRequestException("Email already exists");
    User user = User.builder()
        .name(req.getName()).email(req.getEmail()).phone(req.getPhone())
        .password(passwordEncoder.encode(req.getPassword()))   // hash here
        .role(req.getRole()).status(UserStatus.ACTIVE).build();
    User saved = userRepository.save(user);
    auditLogService.logAction(saved.getUserId(), "CREATE_USER", "User", saved.getUserId());
    return mapToResponse(saved);   // mapToResponse OMITS the password
}

public void deleteUser(Integer id) {           // SOFT delete
    User u = /* find or 404 */;
    u.setStatus(UserStatus.INACTIVE);          // never physically removed
    userRepository.save(u);
    auditLogService.logAction(id, "SOFT_DELETE", "User", id);
}
```

**Talking points:**
- **Soft delete** — deleting sets status to INACTIVE; the record and its audit history survive.
- `mapToResponse` deliberately **omits the password** — the hash never leaves the service.

**Repository — `UserRepository`:** "Spring Data JPA derived queries — no SQL written by hand."
```java
Optional<User> findByEmail(String email);
List<User> findByRoleAndStatus(Role role, UserStatus status);
boolean existsByEmail(String email);
```

**DTOs:** "Clean separation between the wire and the entity."
- `LoginRequest` — validated email + password.
- `LoginResponse` — `token, role, userId, email, name` (what the frontend consumes).
- `UserResponse` — **no password field** — safe outward projection.
- `ApiResponse<T>` — a consistent envelope `{ success, message, data, timestamp }` used by every endpoint.

**Exceptions — `GlobalExceptionHandler` (`@RestControllerAdvice`):** "One place turns exceptions into consistent JSON: `ResourceNotFound → 404`, `BadRequest → 400`, validation errors → 400 with field messages, and `AccessDeniedException → 403` — that last one is what a failed `@PreAuthorize` produces."

**Backend wrap-up line:**
> "So the backend gives us: BCrypt-secured credentials, a signed stateless JWT carrying identity + role, per-endpoint role enforcement, a full audit trail, and a seeded admin to bootstrap it all."

---

# PART 2 — FRONTEND (React, port 5173)

**[SAY] transition:**
> "Now the other half — how the React app consumes all of this. Three files do the heavy lifting: `AuthContext` holds the session, `ProtectedRoute` guards the routes, and `App.jsx` wires the role map. And one thing to keep in mind throughout: **the frontend's authorization is convenience only — the real enforcement is the backend we just saw.**"

## 2.1 Bootstrap — main.jsx (30 sec)

**File:** `frontend/src/main.jsx`
```jsx
ReactDOM.createRoot(document.getElementById('root')).render(
  <BrowserRouter>
    <AuthProvider>          {/* auth state available everywhere */}
      <App />
    </AuthProvider>
  </BrowserRouter>
)
```
> "The whole app is wrapped in `AuthProvider`, so any component can call `useAuth()` to read the current user, token, and login/logout functions."

## 2.2 AuthContext — the session store (2 min)

**File:** `frontend/src/context/AuthContext.jsx`
```jsx
export function AuthProvider({ children }) {
  const [user, setUser]   = useState(null)
  const [token, setToken] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {                       // rehydrate on page load / refresh
    const stored = localStorage.getItem('staynest_auth')
    const tk = localStorage.getItem('token')
    if (stored) { setUser(JSON.parse(stored)); setToken(tk) }
    setLoading(false)
  }, [])

  const login = (data) => {               // data = { token, role, userId, name, email }
    setUser(data); setToken(data.token)
    localStorage.setItem('staynest_auth', JSON.stringify(data))
    localStorage.setItem('token', data.token)
  }

  const logout = () => {
    setUser(null); setToken(null)
    localStorage.removeItem('staynest_auth')
    localStorage.removeItem('token')
  }

  const isAuthenticated = Boolean(user && (token || localStorage.getItem('token')))
  return <AuthContext.Provider value={{ user, token, isAuthenticated, loading, login, logout }}>
           {children}
         </AuthContext.Provider>
}
export const useAuth = () => useContext(AuthContext)
```

**[SAY]**
> "The JWT lives in **localStorage** under `token`, and the user object (including the role) under `staynest_auth`. The `useEffect` rehydrates both on load, so a page refresh doesn't log you out. `loading` is important — it gates the app until rehydration finishes, so a refresh doesn't briefly bounce an authenticated user to the login page. `login` and `logout` just sync React state and localStorage together. And crucially, **`user.role` is the single string that drives all frontend authorization.**"

## 2.3 ProtectedRoute — the guard (2 min)

**File:** `frontend/src/components/ProtectedRoute.jsx`
```jsx
export default function ProtectedRoute({ children, roles }) {
  const { isAuthenticated, user, loading } = useAuth()
  const location = useLocation()

  if (loading) return <Spinner />                         // 1. wait for rehydration

  if (!isAuthenticated)                                    // 2. not logged in
    return <Navigate to="/login" state={{ from: location }} replace />

  if (roles?.length && !roles.includes(user?.role)        // 3. role not allowed
      && user?.role !== 'ADMIN')                           //    (ADMIN bypasses everything)
    return <Navigate to="/unauthorized" replace />

  return children
}
```

**[SAY]**
> "Every protected page passes through here in four steps: wait for `loading`; if not authenticated, redirect to `/login` — and remember where they were trying to go via `state.from`; if the route declares allowed `roles` and the user isn't one of them, send them to `/unauthorized`. **ADMIN short-circuits every check** — it's the superuser. There's also a secondary `ROLE_ACCESS` path map in this file as a defense-in-depth double-check, but the `roles` prop is the primary gate."

## 2.4 App.jsx — the route & role map (1.5 min)

**File:** `frontend/src/App.jsx`
```jsx
const withLayout = (roles, el) => (
  <ProtectedRoute roles={roles}>
    <Layout>{el}</Layout>          {/* sidebar + navbar shell */}
  </ProtectedRoute>
)

<Routes>
  {/* PUBLIC — no guard */}
  <Route path="/login"    element={<LoginPage />} />
  <Route path="/register" element={<RegisterPage />} />
  <Route path="/unauthorized" element={<h3>403 — Unauthorized</h3>} />

  {/* IAM — ADMIN only */}
  <Route path="/users" element={withLayout(['ADMIN'], <UserListPage />)} />

  {/* examples of role-scoped routes */}
  <Route path="/book"       element={withLayout(['GUEST','ADMIN'], <BookingSearchPage />)} />
  <Route path="/front-desk" element={withLayout(['FRONTDESK','ADMIN'], <FrontDeskPage />)} />
  <Route path="/analytics"  element={withLayout(['ADMIN','REVENUEMANAGER'], <AnalyticsDashboardPage />)} />
  {/* ...one guarded route per feature... */}

  <Route path="*" element={<Navigate to="/login" replace />} />   {/* catch-all */}
</Routes>
```

**[SAY]**
> "`App.jsx` is the single source of truth for *which role sees which page*. The `withLayout` helper wraps each page in the `ProtectedRoute` guard **and** the shared `Layout`. Login, register, and unauthorized are the only public routes. Notice **`/users` — the IAM management page — is locked to `['ADMIN']`.** The catch-all at the bottom sends unknown URLs back to login."

## 2.5 Login & Register pages (1.5 min)

**File:** `frontend/src/pages/LoginPage.jsx`
```jsx
const res = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email, password }),
})
const payload = await res.json()
if (!res.ok) throw new Error(payload.message || 'Login failed')

const data = 'data' in payload ? payload.data : payload   // unwrap ApiResponse envelope
login(data)                                                // store in context + localStorage

const home = data.role === 'ADMIN' ? '/users'
           : data.role === 'FRONTDESK' ? '/front-desk'
           : data.role === 'REVENUEMANAGER' ? '/analytics'
           : '/book'
navigate(home)                                             // role-based landing page
```

**[SAY]**
> "Login POSTs straight to `/api/auth/login`, unwraps the `ApiResponse` envelope to get `{ token, role, ... }`, and hands it to `AuthContext.login()`. Then the nice touch: it routes each role to a **different home page** — an admin lands on User Management, a front-desk clerk on the front desk, a guest on booking search."

**File:** `frontend/src/pages/RegisterPage.jsx`
```jsx
if (form.password !== form.confirmPassword) return setError('Passwords do not match')
if (form.password.length < 6) return setError('Password must be at least 6 characters')

await fetch('/api/auth/register', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ name, email, phone, password, role: 'GUEST' }),  // always GUEST
})
```

**[SAY]**
> "Register does client-side validation — password match, minimum length — and **hardcodes `role: 'GUEST'`**. This mirrors the backend's forced override: even though the frontend asks for GUEST, remember the backend enforces it regardless. **Defense in two layers** — the UI is polite about it, the backend is strict about it."

## 2.6 The IAM Module — User Management UI (1.5 min)

**File:** `frontend/src/pages/iam/UserListPage.jsx` (route `/users`, ADMIN-only)

**[SAY]**
> "This is the admin's control panel. On mount it calls `getUsers()` to load everyone into a table with role and status badges, plus a client-side search. The two actions are activate/deactivate and add-user."

```jsx
const { user: currentUser } = useAuth()

const toggleStatus = async (u) => {
  const next = u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  if (next === 'INACTIVE' && u.userId === currentUser?.userId) {
    alert('You cannot deactivate your own account'); return    // mirrors backend rule
  }
  await updateUserStatus(u.userId, next)
  load()
}
```

> "Notice the self-deactivation guard is **repeated on the frontend** for instant feedback — but the backend enforces the same rule, so it holds even if someone bypasses the UI."

**File:** `frontend/src/components/UserFormModal.jsx` — "The Add-User dialog is the **only** place in the whole app where a role can be chosen from the full list — GUEST through ADMIN. And it's only reachable from the ADMIN-guarded `/users` page, so role assignment is effectively admin-only, front to back."
```jsx
const ROLES = ['GUEST','FRONTDESK','HOUSEKEEPING','FBMANAGER','REVENUEMANAGER','ADMIN']
await createUser(form)   // POST /api/users  (backend @PreAuthorize hasRole('ADMIN'))
```

## 2.7 The de-facto interceptor & role-based menu (1.5 min)

**File:** `frontend/src/services/iam/userService.js`

**[SAY]**
> "There's no axios in this project. Instead each service module has this hand-written `request()` wrapper — it's the equivalent of an axios interceptor pair: it **attaches the Bearer token** and **handles 401s**."

```jsx
const request = async (url, options = {}) => {
  const headers = { 'Content-Type': 'application/json' }
  const token = localStorage.getItem('token') || ''
  if (token) headers['Authorization'] = `Bearer ${token}`   // attach JWT to every call

  const res = await fetch(url, { ...options, headers })

  if (res.status === 401) {                                  // token expired / invalid
    localStorage.removeItem('token')
    localStorage.removeItem('staynest_auth')
    window.location.href = '/login'                          // force re-login
    throw new Error('Unauthorized')
  }
  const payload = await res.json()
  if (!res.ok) throw new Error(payload.message || 'Request failed')
  return 'data' in payload ? payload.data : payload
}

export const getUsers         = ()          => request('/api/users')
export const createUser       = (data)      => request('/api/users', { method:'POST', body: JSON.stringify(data) })
export const updateUserStatus = (id, status)=> request(`/api/users/${id}/status?status=${status}`, { method:'PATCH' })
```

**[SAY]**
> "Every call reads the token from localStorage and sets `Authorization: Bearer ...` — exactly the header the backend's `JwtFilter` is looking for. And this is the **session-expiry handler**: a **401** from any endpoint wipes localStorage and hard-redirects to login. That's how a 24-hour-expired token cleanly logs the user out."

**File:** `frontend/src/components/Sidebar.jsx` — role-based menu
```jsx
const ALL_LINKS = [
  { to:'/users',      label:'User Management', roles:['ADMIN'] },
  { to:'/analytics',  label:'Analytics',       roles:['ADMIN','REVENUEMANAGER'] },
  { to:'/front-desk', label:'Front Desk',      roles:['FRONTDESK','ADMIN'] },
  // ...one entry per route, each tagged with allowed roles...
]
const links = ALL_LINKS.filter(l => l.roles.includes(user.role))   // show only what this role can use
```
> "The sidebar filters its links by the user's role — so 'User Management' only appears for admins. But this is **UI hiding only** — if a guest manually types `/users`, `ProtectedRoute` still bounces them to `/unauthorized`, and even then the backend `@PreAuthorize` would reject the API call. **Three layers agreeing.**"

---

## Part 3 — The Full Story in One Breath (closing, 45 sec)

**[SAY] — trace one request end to end:**
> "Let me tie it together with a single login-to-action journey:
> 1. Admin submits login → React `fetch` hits `/api/auth/login`.
> 2. `AuthController` verifies the BCrypt hash and the ACTIVE status, then `JwtUtil` mints a 24-hour HS256 token carrying the email and role.
> 3. `AuthContext.login()` stores it in localStorage and React state; the page routes the admin to `/users`.
> 4. The User Management page calls `getUsers()`, whose `request()` wrapper attaches `Authorization: Bearer <token>`.
> 5. On the backend, `JwtFilter` validates the token, loads `ROLE_ADMIN` into the SecurityContext, and `@PreAuthorize` lets the call through.
> 6. Every mutation writes an `AuditLog`. If the token were expired, the filter rejects it, the frontend gets a 401, wipes storage, and redirects to login.
> That's IAM: **one signed token, trusted everywhere, role enforced at every layer — UI, route, and API.**"

---

## Appendix — Quick Reference

**Backend files (all under `backend/iam-service/src/main/java/com/staynest/iam/`):**
| Concern | File |
|---|---|
| Bootstrap | `IamServiceApplication.java` |
| Config | `src/main/resources/application.yml` |
| Entities | `entity/User.java`, `entity/AuditLog.java` |
| Enums | `enums/Role.java`, `enums/UserStatus.java` |
| Security | `config/SecurityConfig.java`, `config/JwtUtil.java`, `config/JwtFilter.java` |
| Seeding | `config/DataSeeder.java` |
| Controllers | `controller/AuthController.java`, `controller/UserController.java`, `controller/AuditLogController.java` |
| Services | `serviceimpl/UserServiceImpl.java`, `serviceimpl/AuditLogServiceImpl.java` |
| Repos | `repository/UserRepository.java`, `repository/AuditLogRepository.java` |
| DTOs | `dto/LoginRequest.java`, `LoginResponse.java`, `UserRequest.java`, `UserResponse.java`, `ApiResponse.java` |
| Exceptions | `exception/GlobalExceptionHandler.java` |

**Frontend files (under `frontend/src/`):**
| Concern | File |
|---|---|
| Bootstrap | `main.jsx` |
| Routing / role map | `App.jsx` |
| Session state | `context/AuthContext.jsx` |
| Route guard | `components/ProtectedRoute.jsx` |
| Auth pages | `pages/LoginPage.jsx`, `pages/RegisterPage.jsx` |
| IAM UI | `pages/iam/UserListPage.jsx`, `components/UserFormModal.jsx` |
| API + token/401 handling | `services/iam/userService.js` |
| Role-based menu | `components/Sidebar.jsx`, `components/Navbar.jsx` |
| Dev proxy | `vite.config.js` (proxies `/api` → `http://localhost:8090`) |

**Demo credentials:** `admin@staynest.com` / `Admin@123` (seeded).

**Three-layer authorization summary:**
1. **Sidebar** hides links by role (cosmetic).
2. **ProtectedRoute** blocks navigation by role (client convenience).
3. **`@PreAuthorize` + JwtFilter** enforce on the server (the real security).
