# bmx-nverse

**bmx-nverse** is an opinionated, batteries-included Spring Boot security library built on top of Spring Security. It provides a complete, production-ready authentication and authorization stack — JWT-based stateless auth, AES-encrypted email storage, peppered BCrypt password hashing, CORS configuration, multi-layer XSS sanitization, domain-validated request interception, and role-based access control — all wired together with a consistent, generic type model so consuming applications only implement what is unique to them.

- **Group ID:** `com.bloomscorp`
- **Artifact ID:** `bmx-nverse`
- **Current Version:** `3.4.0.2`
- **Java:** 21+
- **Spring Boot:** 3.4.x
- **License:** MIT
- **Published to:** [Maven Central](https://central.sonatype.com/artifact/com.bloomscorp/bmx-nverse)

---

## Table of Contents

- [Why bmx-nverse?](#why-bmx-nverse)
- [Architecture Overview](#architecture-overview)
- [Dependencies](#dependencies)
- [Installation](#installation)
- [Core Concepts](#core-concepts)
  - [The Tenant Model](#the-tenant-model)
  - [The Role Model](#the-role-model)
  - [Email Encryption](#email-encryption)
  - [Password Hashing](#password-hashing)
  - [JWT Authentication](#jwt-authentication)
  - [The Filter Chain](#the-filter-chain)
  - [Gatekeeper and Surveillance](#gatekeeper-and-surveillance)
  - [Sanitization](#sanitization)
  - [Domain Validation](#domain-validation)
  - [Auth Providers](#auth-providers)
- [Integration Guide](#integration-guide)
  - [Step 1 — Define the role enum](#step-1--define-the-role-enum)
  - [Step 2 — Define the role entity](#step-2--define-the-role-entity)
  - [Step 3 — Define the tenant entity](#step-3--define-the-tenant-entity)
  - [Step 4 — Implement NVerseTenantDAO](#step-4--implement-nversetenantdao)
  - [Step 5 — Implement NVerseUserRoleDAO](#step-5--implement-nverseuserroledao)
  - [Step 6 — Wire up the Security Configuration](#step-6--wire-up-the-security-configuration)
  - [Step 7 — Build the Login Endpoint](#step-7--build-the-login-endpoint)
  - [Step 8 — Protect Endpoints with the Gatekeeper](#step-8--protect-endpoints-with-the-gatekeeper)
  - [Step 9 — Sanitize Request Input](#step-9--sanitize-request-input)
  - [Step 10 — Apply Domain Validation](#step-10--apply-domain-validation)
- [API Reference](#api-reference)
  - [NVerseTenant](#nversetenant)
  - [NVerseRole](#nverserole)
  - [NVERSE_AUTH_PROVIDER](#nverse_auth_provider)
  - [NVerseAES](#nverseaes)
  - [NVerseEmailEncoder](#nverseemailencoder)
  - [NVerseEmailValidator](#nverseemailvalidator)
  - [NVersePasswordEncoder](#nversepasswordencoder)
  - [NVerseJWTService](#nversejwtservice)
  - [NVerseAuthenticationService](#nverseauthenticationservice)
  - [NVerseAuthorityResolver](#nverseauthorityresolver)
  - [NVerseUserDetailsService](#nverseuserdetailsservice)
  - [NVerseUser](#nverseuser)
  - [NVerseGatekeeper](#nversegatekeeper)
  - [NVerseSecureLayer](#nversesecurelayer)
  - [NVerseSurveillanceReport](#nversesurveillancereport)
  - [NVerseSecurityFilterChain](#nversesecurityfilterchain)
  - [NVerseCORSConfigurationSource](#nversecorsconfigurationsource)
  - [NVerseExceptionHandlerFilter](#nverseexceptionhandlerfilter)
  - [NVerseRequestFilter](#nverserequestfilter)
  - [NVerseHttpRequestFilter](#nversehttprequestfilter)
  - [NVerseHttpRequestWrapper](#nversehttprequestwrapper)
  - [NVerseCachedServletInputStream](#nversecachedservletinputstream)
  - [NVerseSanitizer](#nversesanitizer)
  - [HttpRequestDumpSanitizer](#httprequestdumpsanitizer)
  - [NVerseDomainValidated](#nversedomainvalidated)
  - [NVerseValidationAspect](#nversevalidationaspect)
  - [DomainValidator](#domainvalidator)
  - [NVerseValidator](#nversevalidator)
  - [NVerseTenantDAO](#nversetenantdao)
  - [NVerseUserRoleDAO](#nverseuserroledao)
  - [NVerseRequest](#nverserequest)
  - [NVerseResponse](#nverseresponse)
  - [NVerseProviderResponse](#nverseproviderresponse)
  - [NVerseAuthProviderValidationRequest](#nverseauthprovidervalidationrequest)
  - [Constant](#constant)
  - [Message](#message)
- [Package Structure](#package-structure)
- [Design Notes](#design-notes)
- [Dependencies Table](#dependencies-table)
- [License](#license)

---

## Why bmx-nverse?

Every Spring Boot application that handles users needs the same security scaffolding: JWTs, password hashing, CORS, filter ordering, exception-to-JSON mapping, input sanitization, and role checks. Each team re-implements this independently, producing divergent security surfaces, inconsistent error responses, and repeated boilerplate.

bmx-nverse solves this by providing a single, audited, generic implementation of the full security stack:

- **One security contract** — the `NVerseTenant` and `NVerseRole` interfaces define what a user and a role look like. Implement them in your JPA entities and the library wires everything together.
- **Encrypted email identity** — email addresses are stored AES-encrypted in the database. The library handles encryption before queries and decryption transparently; the application only ever sees plaintext emails.
- **Peppered BCrypt** — passwords are strengthened with a secret pepper before BCrypt hashing. Database theft without the pepper produces useless hashes.
- **Fully configured filter chain** — HTTPS enforcement, CSRF disabled, CORS, stateless sessions, JWT validation filter, exception handling filter, and request body caching filter, assembled in correct order.
- **Centralized exception-to-JSON** — every security exception (expired token, bad credentials, locked account, forged request, etc.) is caught in one place and serialized to a consistent JSON failure response via bmx-raintree.
- **Gatekeeper and surveillance** — a reusable role-based authorization API with an embedded integrity check that detects forged user objects before running permission logic.
- **Multi-layer XSS sanitization** — regex-based XSS stripping + OWASP HTML Sanitizer, applicable to individual strings or entire DTOs via reflection.
- **Aspect-based domain validation** — annotate a controller class with `@NVerseDomainValidated` to validate the request origin or custom headers before the handler runs.

---

## Architecture Overview

```
HTTP Request
     │
     ▼
NVerseExceptionHandlerFilter   ← catches all security exceptions → JSON response (bmx-raintree)
     │
     ▼
NVerseRequestFilter            ← validates JWT, populates SecurityContext
     │
     ▼
Spring Security AuthorizationFilter
     │
     ▼
NVerseHttpRequestFilter        ← wraps request for repeatable body reads
     │
     ▼
Controller / Service
     │  ├─ NVerseGatekeeper.runSurveillance()    ← integrity + permission check
     │  ├─ NVerseSanitizer.sanitize()             ← XSS + HTML sanitization
     │  └─ @NVerseDomainValidated (AOP)           ← origin/header validation
     ▼
Response
```

The generic type parameters used throughout the library are:

| Parameter | Bound | Role |
|---|---|---|
| `T` | `NVerseTenant<E, R>` | Your user/tenant JPA entity |
| `E` | `Enum<E>` | Your role enum |
| `R` | `NVerseRole<E>` | Your role JPA entity |
| `B` | `LogBook<L, A, T, E, R>` | Your bmx-alfred LogBook subclass |
| `L` | `Log` | Your log JPA entity |
| `A` | `AuthenticationLog` | Your authentication log JPA entity |

---

## Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| `org.springframework.boot:spring-boot-starter-web` | 3.4.0 | Web MVC and servlet support |
| `org.springframework.boot:spring-boot-starter-security` | 3.4.0 | Spring Security core |
| `org.springframework.boot:spring-boot-starter-data-jpa` | 3.4.0 | JPA/Hibernate ORM support |
| `io.jsonwebtoken:jjwt-api` | 0.11.5 | JWT generation and parsing API |
| `io.jsonwebtoken:jjwt-impl` | 0.11.5 | JWT implementation (runtime) |
| `io.jsonwebtoken:jjwt-jackson` | 0.11.5 | JWT Jackson integration |
| `com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer` | 20220608.1 | HTML sanitization |
| `com.bloomscorp:bmx-pastebox-java` | 0.0.3 | String utilities, reflection helpers |
| `com.bloomscorp:bmx-hastar` | 0.0.3 | Exception types, ActionCode, ErrorCode, Message |
| `com.bloomscorp:bmx-raintree` | 0.0.14 | Structured JSON response builder |
| `com.bloomscorp:bmx-alfred` | 0.0.14 | Database-backed structured logging |
| `org.projectlombok:lombok` | 1.18.36 | Constructor and accessor generation |
| `org.jetbrains:annotations` | 26.0.1 | Nullability annotations |

---

## Installation

### Maven

```xml
<dependency>
    <groupId>com.bloomscorp</groupId>
    <artifactId>bmx-nverse</artifactId>
    <version>3.4.0.2</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
implementation("com.bloomscorp:bmx-nverse:3.4.0.2")
```

### Gradle (Groovy DSL)

```groovy
implementation 'com.bloomscorp:bmx-nverse:3.4.0.2'
```

---

## Core Concepts

### The Tenant Model

A **tenant** is NVerse's term for a user account. Every application must provide a JPA entity that implements `NVerseTenant<E, R>`. The interface requires getters and setters for:

- `id` — auto-generated database primary key.
- `uid` — a universally unique identifier (e.g., UUID) for the tenant.
- `email` — the AES-encrypted email address used as the authentication username.
- `password` — the BCrypt-encoded peppered password.
- `contactNumber` — optional phone number.
- `isActive` / `setActive` — controls whether the account can authenticate.
- `isDeleted` — soft-delete flag; deleted accounts fail the account-non-expired check.
- `isSuspended` — suspension flag; suspended accounts fail the credentials-non-expired check.
- `getProvider` — which auth provider created the account (`BASIC`, `GOOGLE`, `FACEBOOK`, or `UNKNOWN`).
- `getRoles` — the list of role entities assigned to this tenant.
- `setCreationTime` — records when the account was created.
- `setProfileImageUrl` — stores a profile image URL.

### The Role Model

Every application must provide a JPA entity implementing `NVerseRole<E>`, where `E` is a custom enum whose constants represent the available roles. NVerse maps each role's enum constant name to a Spring Security `SimpleGrantedAuthority`, so role names are used directly in Spring Security expressions.

Two role names have special meaning in the `NVerseGatekeeper`:
- `ROLE_GOD_MODE` — highest privilege; passes all role checks.
- `ROLE_SUPER_USER` — elevated privilege; passes the `roleSU()` check.

### Email Encryption

NVerse **never stores plaintext email addresses** in the database. Instead:

1. At registration, the application calls `NVerseEmailEncoder.encode(email)` which validates the email format with `NVerseEmailValidator` and then AES-encrypts it using `NVerseAES` (AES/ECB/PKCS5Padding with a 128-bit key derived from a configurable passphrase via SHA-512).
2. The encrypted email is stored in the `email` column.
3. At login, the same encryption is applied to the user-supplied email before querying the database. Because ECB mode is deterministic, the same plaintext always produces the same ciphertext, making the encrypted value a stable lookup key.
4. The JWT subject claim holds the encrypted email, so token-based lookups skip encryption and query directly.

**Configuration:** The encoder key and algorithm are supplied to `NVerseEmailEncoder` at construction time and should come from application properties — never hardcoded.

### Password Hashing

`NVersePasswordEncoder` extends Spring Security's `BCryptPasswordEncoder` and adds a **pepper**: a secret string prepended to every password before BCrypt hashing.

- The pepper is stored outside the database (environment variable or secrets manager).
- BCrypt work factor and a `SecureRandom` instance are configurable.
- If the database is stolen without the pepper, offline dictionary attacks are not viable against the hashes.

**Important:** The pepper must never change after passwords are hashed — changing it invalidates all existing credentials.

### JWT Authentication

`NVerseJWTService` handles JWT operations:

- **Generation** — `generateToken(NVerseUser)` produces a compact HS512-signed JWT with the encrypted email as the subject and an expiry of `jwtTokenValidity` milliseconds from now.
- **Validation** — `validateToken(token, userDetails)` checks that the token subject matches the loaded user's username and the token has not expired.
- **Extraction** — `getUsernameFromToken(token)` returns the subject claim (encrypted email).

The signing key is a Base64-encoded HMAC-SHA-512 key supplied via configuration. At least 64 bytes of raw key material is recommended.

### The Filter Chain

NVerse assembles its filter chain through `NVerseSecurityFilterChain.filterChain(...)`. The three NVerse-specific filters, in execution order, are:

1. **`NVerseExceptionHandlerFilter`** (before `NVerseRequestFilter`) — wraps the entire downstream chain in a try/catch that converts security exceptions to `401 Unauthorized` JSON responses via bmx-raintree. Also asynchronously logs every exception via bmx-alfred `CronManager`.

2. **`NVerseRequestFilter`** (before `UsernamePasswordAuthenticationFilter`) — extracts the JWT from the `Authorization: Bearer` header, validates it, loads the user via `NVerseUserDetailsService`, and populates the `SecurityContextHolder`.

3. **`NVerseHttpRequestFilter`** (after Spring's `AuthorizationFilter`) — wraps the request in `NVerseHttpRequestWrapper` so the body can be read multiple times by sanitizers, validators, and the framework's JSON deserializer.

Additionally, the filter chain enforces:
- HTTPS on all requests.
- CSRF disabled (stateless JWT makes CSRF tokens unnecessary).
- Stateless session management (no `HttpSession` created).
- Configurable public URL patterns that bypass authentication.

### Gatekeeper and Surveillance

`NVerseGatekeeper<T, E, R>` is the authorization layer for protected business operations. Applications extend it and implement `userHasAppropriateAuthority(T user, int code)` to define permission logic per operation code.

The `runSurveillance(user, code)` method runs a two-stage pipeline:
1. **Embedded integrity check** (`NVerseSecureLayer.initialEmbeddedCheck`) — verifies the user object is not null, has a positive `id`, and has a non-blank `uid`. Failure returns `ErrorCode.FORGED_REQUEST`.
2. **Authority check** (`userHasAppropriateAuthority`) — application-defined permission check. Failure returns `ErrorCode.AUTHORIZATION_DENIED`.

The result is an `NVerseSurveillanceReport(failed, errorCode)` that the caller inspects before proceeding.

### Sanitization

`NVerseSanitizer<E, R>` provides a three-stage sanitization pipeline:

1. **Canonicalization** — strips null bytes (`\0`).
2. **XSS stripping** — removes `<script>`, `<iframe>`, inline `src=`, `eval()`, `expression()`, `javascript:`, `vbscript:`, and `onload=` patterns using compiled regex.
3. **OWASP HTML sanitization** — passes the value through the OWASP Java HTML Sanitizer, allowing only safe formatting, block, image, link, style, and table elements.

- `sanitize(String)` — sanitizes a single string through all three stages.
- `sanitizeObject(O object, Class<?>... fieldTypes)` — uses reflection to sanitize all fields of the given types on an object in place.

`HttpRequestDumpSanitizer` extends `NVerseSanitizer` to read and sanitize the full HTTP request body, producing a safe string for storage in log `data_dump` columns.

### Domain Validation

The `@NVerseDomainValidated` annotation and `NVerseValidationAspect` provide aspect-oriented request interception for controllers:

- **Domain-only mode** (no `headerKeys`) — validates that the `Origin` header is in the configured `DomainValidator` allowed-domains list.
- **Custom header mode** (`headerKeys` + `headerValues`) — validates that specific request headers match expected values. Can optionally chain a domain validation pass afterwards.

When validation fails, the aspect short-circuits with a failure response without invoking the controller method. The failure response format is inferred from the method's declared return type (`RainTreeResponse` or `String`).

### Auth Providers

`NVERSE_AUTH_PROVIDER` is an enum representing how a tenant account was created:

| Constant | Meaning |
|---|---|
| `UNKNOWN` | Provider not determined |
| `BASIC` | Standard username/password registration |
| `GOOGLE` | OAuth via Google |
| `FACEBOOK` | OAuth via Facebook |

**The ordinal order is fixed and must not be changed** — persisted integer representations depend on declaration order.

`NVerseAuthenticationService.validateAuthProvider(tenant, provider, allowUnknown)` checks whether a tenant's stored provider matches the expected value. The `allowUnknown` flag controls whether `UNKNOWN` is considered a valid match.

---

## Integration Guide

### Step 1 — Define the role enum

```java
public enum AppRole {
    ROLE_GOD_MODE,
    ROLE_SUPER_USER,
    ROLE_ADMIN,
    ROLE_USER
}
```

### Step 2 — Define the role entity

```java
@Entity
@Table(name = "user_role")
@Getter
@Setter
public class AppUserRole implements NVerseRole<AppRole> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private AppRole role;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;
}
```

### Step 3 — Define the tenant entity

```java
@Entity
@Table(name = "app_user")
@Getter
@Setter
public class AppUser implements NVerseTenant<AppRole, AppUserRole> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uid", nullable = false, unique = true)
    private String uid;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "is_suspended", nullable = false)
    private boolean suspended = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private NVERSE_AUTH_PROVIDER provider = NVERSE_AUTH_PROVIDER.BASIC;

    @Column(name = "creation_time")
    private long creationTime;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<AppUserRole> roles = new ArrayList<>();
}
```

### Step 4 — Implement NVerseTenantDAO

```java
@Repository
public class AppUserDAO implements NVerseTenantDAO<AppUser, AppRole, AppUserRole> {

    private final AppUserRepository userRepository;
    private final NVerseEmailEncoder emailEncoder;

    public AppUserDAO(AppUserRepository userRepository, NVerseEmailEncoder emailEncoder) {
        this.userRepository = userRepository;
        this.emailEncoder = emailEncoder;
    }

    @Override
    public boolean verifyUniqueEmail(String email, boolean encoded) {
        try {
            String lookupEmail = encoded ? email : this.emailEncoder.encode(email);
            return !this.userRepository.existsByEmail(lookupEmail);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean verifyUniqueContactNumber(String contactNumber) {
        return !this.userRepository.existsByContactNumber(contactNumber);
    }

    @Override
    public AppUser retrieveUserByEmail(String username) {
        return this.userRepository.findByEmail(username).orElse(null);
    }

    @Override
    public int addNewTenant(AppUser user, AppUserRole userRole) {
        return this.addNewTenant(user, userRole, true, true);
    }

    @Override
    public int addNewTenant(AppUser user, AppUserRole userRole, boolean verifyUniqueEmail, boolean verifyUniqueContactNumber) {
        if (verifyUniqueEmail && !this.verifyUniqueEmail(user.getEmail(), true))
            return ActionCode.NOT_UNIQUE;
        try {
            AppUser saved = this.userRepository.save(user);
            userRole.setUser(saved);
            return ActionCode.INSERT_SUCCESS;
        } catch (Exception e) {
            return ActionCode.INSERT_FAILURE;
        }
    }
}
```

### Step 5 — Implement NVerseUserRoleDAO

```java
@Repository
public class AppUserRoleDAO implements NVerseUserRoleDAO<AppRole, AppUserRole> {

    private final AppUserRoleRepository roleRepository;

    public AppUserRoleDAO(AppUserRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public int addNewRole(AppUserRole role) {
        try {
            this.roleRepository.save(role);
            return ActionCode.INSERT_SUCCESS;
        } catch (Exception e) {
            return ActionCode.INSERT_FAILURE;
        }
    }
}
```

### Step 6 — Wire up the Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Inject from application.properties — never hardcode secrets
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.validity-ms}")
    private long jwtValidity;

    @Value("${app.email.encoder-key}")
    private String emailEncoderKey;

    @Value("${app.password.pepper}")
    private String passwordPepper;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public NVerseEmailValidator emailValidator() {
        return new NVerseEmailValidator();
    }

    @Bean
    public NVerseEmailEncoder emailEncoder() {
        return new NVerseEmailEncoder(emailEncoderKey, emailValidator(), NVerseAES.SHA512);
    }

    @Bean
    public NVersePasswordEncoder passwordEncoder() {
        return new NVersePasswordEncoder(12, new SecureRandom(), passwordPepper);
    }

    @Bean
    public NVerseJWTService<AppUser, AppRole, AppUserRole> jwtService() {
        return new NVerseJWTService<>(jwtSecret, jwtValidity);
    }

    @Bean
    public NVerseTenantDAO<AppUser, AppRole, AppUserRole> tenantDAO(
        AppUserRepository userRepo,
        NVerseEmailEncoder encoder
    ) {
        return new AppUserDAO(userRepo, encoder);
    }

    @Bean
    public NVerseUserDetailsService<AppUser, AppRole, AppUserRole> userDetailsService(
        NVerseEmailEncoder encoder,
        NVerseTenantDAO<AppUser, AppRole, AppUserRole> dao
    ) {
        return new NVerseUserDetailsService<>(encoder, dao);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public NVerseAuthenticationEntryPoint authenticationEntryPoint() {
        return new NVerseAuthenticationEntryPoint();
    }

    @Bean
    public RainTree rainTree() {
        return new RainTree();
    }

    @Bean
    public NVerseRequestFilter<AppUser, AppRole, AppUserRole> requestFilter(
        NVerseJWTService<AppUser, AppRole, AppUserRole> jwt,
        NVerseUserDetailsService<AppUser, AppRole, AppUserRole> uds
    ) {
        return new NVerseRequestFilter<>(jwt, uds);
    }

    @Bean
    public NVerseExceptionHandlerFilter<AppLogBook, AppLog, AppAuthLog, AppUser, AppRole, AppUserRole> exceptionHandlerFilter(
        RainTree rainTree,
        CronManager<AppLogBook, AppLog, AppAuthLog, AppUser, AppRole, AppUserRole> cron
    ) {
        return new NVerseExceptionHandlerFilter<>(rainTree, cron, true); // true = production mode
    }

    @Bean
    public NVerseHttpRequestFilter httpRequestFilter() {
        return new NVerseHttpRequestFilter();
    }

    @Bean
    public NVerseCORSConfigurationSource corsConfigurationSource() {
        return new NVerseCORSConfigurationSource();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        NVerseAuthenticationEntryPoint entryPoint,
        NVerseRequestFilter<AppUser, AppRole, AppUserRole> requestFilter,
        NVerseExceptionHandlerFilter<AppLogBook, AppLog, AppAuthLog, AppUser, AppRole, AppUserRole> exceptionFilter,
        NVerseHttpRequestFilter httpRequestFilter,
        NVerseCORSConfigurationSource corsSource
    ) throws Exception {

        // Register CORS configuration source with Spring Security
        http.cors().configurationSource(corsSource.source(allowedOrigins));

        return new NVerseSecurityFilterChain<AppLogBook, AppLog, AppAuthLog, AppUser, AppRole, AppUserRole>()
            .filterChain(
                http,
                new String[]{"/api/auth/**", "/api/public/**"},
                "/**",
                entryPoint,
                requestFilter,
                exceptionFilter,
                httpRequestFilter
            );
    }
}
```

### Step 7 — Build the Login Endpoint

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final NVerseAuthenticationService authService;
    private final NVerseUserDetailsService<AppUser, AppRole, AppUserRole> userDetailsService;
    private final NVerseJWTService<AppUser, AppRole, AppUserRole> jwtService;
    private final AuthenticationManager authenticationManager;
    private final RainTree rainTree;

    // constructor injection...

    @PostMapping("/login")
    public String login(@RequestBody NVerseRequest request) {

        // 1. Authenticate username/password via Spring Security
        this.authService.authenticate(request.username(), request.password(), authenticationManager);

        // 2. Load the user to generate a token
        NVerseUser<AppUser, AppRole, AppUserRole> user =
            this.userDetailsService.loadUserByUsername(request.username());

        // 3. Generate JWT
        String token = this.jwtService.generateToken(user);

        // 4. Return the token in a RainTree response
        return this.rainTree.renderResponse(new NVerseResponse(token));
    }

    @PostMapping("/register")
    public RainTreeResponse register(@RequestBody AppRegistrationRequest request) {
        // ... build AppUser entity, encrypt email, encode password, then:
        int result = tenantDAO.addNewTenant(user, userRole);
        return RainResponse.prepareActionResponse(result);
    }
}
```

### Step 8 — Protect Endpoints with the Gatekeeper

```java
@Component
public class AppGatekeeper extends NVerseGatekeeper<AppUser, AppRole, AppUserRole> {

    // Operation codes — define your own integer constants per domain
    public static final int CREATE_CONTENT = 101;
    public static final int DELETE_CONTENT = 102;
    public static final int VIEW_REPORTS   = 201;

    public AppGatekeeper(NVerseSecureLayer<AppUser, AppRole, AppUserRole> secureLayer) {
        super(secureLayer);
    }

    @Override
    public boolean userHasAppropriateAuthority(AppUser user, int code) {
        if (this.roleSU(user)) return true; // super users can do anything

        return switch (code) {
            case CREATE_CONTENT -> user.getRoles().stream()
                .anyMatch(r -> r.getRole() == AppRole.ROLE_ADMIN || r.getRole() == AppRole.ROLE_USER);
            case DELETE_CONTENT -> user.getRoles().stream()
                .anyMatch(r -> r.getRole() == AppRole.ROLE_ADMIN);
            case VIEW_REPORTS -> user.getRoles().stream()
                .anyMatch(r -> r.getRole() == AppRole.ROLE_ADMIN);
            default -> false;
        };
    }
}
```

Using the gatekeeper in a service:

```java
@Service
public class ContentService {

    private final AppGatekeeper gatekeeper;
    private final RainTree rainTree;
    private final NVerseAuthorityResolver<AppUser, AppRole, AppUserRole> authorityResolver;

    // constructor injection...

    public String createContent(
        NVerseHttpRequestWrapper request,
        String authorizationHeader,
        ContentRequest contentRequest
    ) {
        // 1. Resolve the authenticated tenant from the Authorization header
        AppUser currentUser = this.authorityResolver
            .resolveUserInformationFromAuthorizationToken(authorizationHeader);

        // 2. Run the surveillance pipeline
        NVerseSurveillanceReport report = this.gatekeeper.runSurveillance(
            currentUser,
            AppGatekeeper.CREATE_CONTENT
        );

        if (report.failed())
            return this.rainTree.failureResponse(ErrorCode.decode(report.errorCode()));

        // 3. Proceed with business logic
        // ...
        return this.rainTree.successResponse("Content created.");
    }
}
```

### Step 9 — Sanitize Request Input

For a single string field:

```java
// Extend NVerseSanitizer for your DTO type
public class ContentRequestSanitizer extends NVerseSanitizer<ContentRequest, ContentRequest> {

    @Override
    public ContentRequest getSanitized(ContentRequest request) {
        return this.sanitizeObject(request, String.class);
    }
}
```

```java
// Usage in a controller or service
ContentRequest sanitized = new ContentRequestSanitizer().getSanitized(rawRequest);
```

For logging the raw request body safely:

```java
HttpRequestDumpSanitizer dumpSanitizer = new HttpRequestDumpSanitizer();
String sanitizedDump = dumpSanitizer.getSanitized(httpRequestWrapper);
// Use sanitizedDump as the data_dump value in a log entry
```

### Step 10 — Apply Domain Validation

Annotate the controller class. The annotation is read from the class level by the aspect.

```java
// Domain-only validation: check that the Origin header is in the allowed-domains list
@RestController
@RequestMapping("/api/content")
@NVerseDomainValidated
public class ContentController {

    @PostMapping
    public RainTreeResponse create(NVerseHttpRequestWrapper request, @RequestBody ContentRequest body) {
        // Only reached if the Origin header is in the DomainValidator's allowed list
        return contentService.create(body);
    }
}
```

```java
// Custom header validation: require a specific API key header
@RestController
@RequestMapping("/api/webhook")
@NVerseDomainValidated(
    headerKeys   = {"X-Webhook-Secret"},
    headerValues = {"${app.webhook.secret}"}
)
public class WebhookController {

    @PostMapping
    public RainTreeResponse receive(NVerseHttpRequestWrapper request, @RequestBody WebhookPayload payload) {
        // Only reached if X-Webhook-Secret matches the configured value
        return webhookService.process(payload);
    }
}
```

Register the aspect and domain validator as Spring beans:

```java
@Configuration
public class ValidationConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public DomainValidator domainValidator() {
        return new DomainValidator(Arrays.asList(allowedOrigins.split(",")));
    }

    @Bean
    public NVerseValidationAspect validationAspect(DomainValidator domainValidator, RainTree rainTree) {
        return new NVerseValidationAspect(domainValidator, rainTree);
    }
}
```

---

## API Reference

### `NVerseTenant`

**Package:** `com.bloomscorp.nverse.pojo`

Core interface that every application user entity must implement. Provides the identity, account state, and roles contract consumed by the NVerse authentication stack.

| Method | Description |
|---|---|
| `getId()` | Database primary key; must be `> 0` for a valid tenant. |
| `getUid()` / `setUid(String)` | Universally unique identifier; must not be blank. |
| `getEmail()` / `setEmail(String)` | AES-encrypted email address as stored in the database. |
| `getPassword()` / `setPassword(String)` | BCrypt-encoded peppered password. |
| `getContactNumber()` | Phone number; may be `null`. |
| `isActive()` / `setActive(boolean)` | Account enabled state for Spring Security. |
| `isDeleted()` | Soft-delete flag; deleted accounts fail account-non-expired check. |
| `isSuspended()` | Suspension flag; suspended accounts fail credentials-non-expired check. |
| `getProvider()` | The `NVERSE_AUTH_PROVIDER` used at registration. |
| `getRoles()` | List of role entities; must not be `null`. |
| `setCreationTime(long)` | Unix-epoch creation timestamp in milliseconds. |
| `setProfileImageUrl(String)` | Absolute URL of the profile image. |

---

### `NVerseRole`

**Package:** `com.bloomscorp.nverse.pojo`

Marker interface for role entities.

| Method | Description |
|---|---|
| `getId()` | Database primary key of the role record. |
| `getRole()` | The enum constant representing the role value. |

---

### `NVERSE_AUTH_PROVIDER`

**Package:** `com.bloomscorp.nverse.pojo`

Enum of supported authentication providers. Ordinal order is fixed.

| Constant | Meaning |
|---|---|
| `UNKNOWN` | Provider not determined. |
| `BASIC` | Username/password authentication. |
| `GOOGLE` | OAuth via Google. |
| `FACEBOOK` | OAuth via Facebook. |

---

### `NVerseAES`

**Package:** `com.bloomscorp.nverse`

AES/ECB/PKCS5Padding symmetric cipher. Derives a 128-bit key from a passphrase using the specified digest algorithm.

| Method | Description |
|---|---|
| `NVerseAES(String keySequence, String algorithm)` | Constructs the cipher. Throws `NoSuchAlgorithmException` for unknown algorithms. |
| `encrypt(String message)` | Returns Base64-encoded ciphertext. |
| `decrypt(String encodedMessage)` | Returns plaintext from Base64-encoded ciphertext. |
| `SHA256` | Algorithm constant for SHA-256. |
| `SHA512` | Algorithm constant for SHA-512. |

---

### `NVerseEmailEncoder`

**Package:** `com.bloomscorp.nverse`

Validates and AES-encrypts email addresses before storage; decrypts and compares them at login.

| Method | Description |
|---|---|
| `encode(String email)` | Validates and encrypts the email. Returns Base64 ciphertext. |
| `decode(String encryptedEmail)` | Decrypts to plaintext email. |
| `matches(String rawEmail, String encodedEmail)` | Tests whether a plaintext email matches an encrypted one. Returns `false` on any exception. |

---

### `NVerseEmailValidator`

**Package:** `com.bloomscorp.nverse`

Validates email format using a compiled regex before encryption.

| Method | Description |
|---|---|
| `getValidatedEmail(String email)` | Returns the email unchanged if valid; `null` if invalid or on exception. |

---

### `NVersePasswordEncoder`

**Package:** `com.bloomscorp.nverse`

Extends `BCryptPasswordEncoder` with a secret pepper prepended before hashing.

| Method | Description |
|---|---|
| `encode(String rawPassword)` | Encodes `pepper + rawPassword` with BCrypt. |
| `matches(CharSequence rawPassword, String encodedPassword)` | Verifies `pepper + rawPassword` against the stored hash. |

---

### `NVerseJWTService`

**Package:** `com.bloomscorp.nverse`

JWT lifecycle management — generation, subject extraction, and validation.

| Method | Description |
|---|---|
| `generateToken(NVerseUser user)` | Generates a signed HS512 JWT with the user's username as subject. |
| `getUsernameFromToken(String token)` | Extracts the subject claim (encrypted email) from the token. |
| `validateToken(String token, UserDetails userDetails)` | Returns `true` if the subject matches and the token is not expired. |

---

### `NVerseAuthenticationService`

**Package:** `com.bloomscorp.nverse`

Utility service for authentication and provider validation at login endpoints.

| Method | Description |
|---|---|
| `authenticate(String username, String password, AuthenticationManager)` | Delegates to Spring Security's `AuthenticationManager`. Throws `DisabledException` or `BadCredentialsException` on failure. |
| `validateAuthProvider(T tenant, NVERSE_AUTH_PROVIDER provider, Boolean allowUnknown)` | Returns `true` if the tenant's provider matches `provider` under the specified leniency. |

---

### `NVerseAuthorityResolver`

**Package:** `com.bloomscorp.nverse`

Resolves a fully loaded tenant from a raw `Authorization` header value.

| Method | Description |
|---|---|
| `resolveUserInformationFromAuthorizationToken(String authorizationToken)` | Strips the `Bearer ` prefix, extracts the JWT subject, loads and returns the tenant. Throws `AuthorizationException(AECx02)` if `authorizationToken` is `null`. |

---

### `NVerseUserDetailsService`

**Package:** `com.bloomscorp.nverse`

Spring Security `UserDetailsService` implementation for the NVerse tenant model.

| Method | Description |
|---|---|
| `loadUserByUsername(String username)` | Encrypts the plaintext email and loads the tenant. Used by Spring Security's `AuthenticationManager`. |
| `loadUserByEncryptedUsername(String username)` | Loads the tenant directly using an already-encrypted email. Used by `NVerseRequestFilter`. |
| `loadNVerseUserByEncryptedUsername(String username)` | Primary database query path. Throws `UsernameNotFoundException` if not found. |

---

### `NVerseUser`

**Package:** `com.bloomscorp.nverse`

Wraps a `NVerseTenant` into Spring Security's `User` type, mapping account flags and roles to `UserDetails` properties. Exposes the original tenant via `getTenant()`.

---

### `NVerseGatekeeper`

**Package:** `com.bloomscorp.nverse`

Abstract authorization guard. Extend this class and implement `userHasAppropriateAuthority`.

| Method | Description |
|---|---|
| `roleGOD(T user)` | Returns `true` if the user has the `ROLE_GOD_MODE` role. |
| `roleSU(T user)` | Returns `true` if the user has `ROLE_GOD_MODE` or `ROLE_SUPER_USER`. |
| `runSurveillance(T user, int code)` | Runs the integrity check then the authority check. Returns an `NVerseSurveillanceReport`. |
| `userHasAppropriateAuthority(T user, int code)` | **Abstract.** Application-defined permission logic. |

---

### `NVerseSecureLayer`

**Package:** `com.bloomscorp.nverse`

Performs the initial embedded integrity check on a tenant object.

| Method | Description |
|---|---|
| `initialEmbeddedCheck(T user)` | Returns `true` if user is non-null, has a positive `id`, and a non-blank `uid`. |

---

### `NVerseSurveillanceReport`

**Package:** `com.bloomscorp.nverse`

Immutable result of `NVerseGatekeeper.runSurveillance`.

| Field | Description |
|---|---|
| `failed()` | `true` if any check did not pass. |
| `errorCode()` | Error code from `ErrorCode`, or `ActionCode.NO_ACTION` on success. |

---

### `NVerseSecurityFilterChain`

**Package:** `com.bloomscorp.nverse`

Factory that assembles the full NVerse Spring Security filter chain.

| Method | Description |
|---|---|
| `filterChain(HttpSecurity, String[], String, ...)` | Builds and returns the `SecurityFilterChain` with HTTPS, CSRF-off, CORS, stateless sessions, and the three NVerse filters in correct order. |

---

### `NVerseCORSConfigurationSource`

**Package:** `com.bloomscorp.nverse`

Produces a `CorsConfigurationSource` allowing credentials, a broad set of headers, and all standard HTTP methods for the specified origins.

| Method | Description |
|---|---|
| `source(String uiOrigins)` | Takes a comma-separated origin list and returns a `UrlBasedCorsConfigurationSource` for `/**`. |

---

### `NVerseExceptionHandlerFilter`

**Package:** `com.bloomscorp.nverse`

Catches all security exceptions propagated through the filter chain and converts them to `401 Unauthorized` JSON responses. Also asynchronously logs every exception via bmx-alfred.

---

### `NVerseRequestFilter`

**Package:** `com.bloomscorp.nverse`

Validates the JWT in the `Authorization: Bearer` header and populates the Spring Security context. Rejects requests with an already-populated context or missing/invalid tokens.

---

### `NVerseHttpRequestFilter`

**Package:** `com.bloomscorp.nverse`

Wraps each request in `NVerseHttpRequestWrapper` to enable repeatable body reads. Registered after the Spring Security authorization filter.

---

### `NVerseHttpRequestWrapper`

**Package:** `com.bloomscorp.nverse`

`HttpServletRequestWrapper` that caches the request body in a `ByteArrayOutputStream` so it can be read multiple times.

---

### `NVerseCachedServletInputStream`

**Package:** `com.bloomscorp.nverse`

`ServletInputStream` backed by a `ByteArrayInputStream` over the cached body. Always `isReady() = true`; does not support async read listeners.

---

### `NVerseSanitizer`

**Package:** `com.bloomscorp.nverse.sanitizer`

Abstract multi-layer XSS sanitizer.

| Method | Description |
|---|---|
| `sanitize(String value)` | Runs canonicalization → XSS stripping → OWASP HTML sanitization on a single string. |
| `sanitizeObject(O object, Class<?>... fieldTypes)` | Sanitizes all fields of the given types on an object in place using reflection. |
| `getSanitized(E entity)` | **Abstract.** Subclasses define their input/output contract. |

---

### `HttpRequestDumpSanitizer`

**Package:** `com.bloomscorp.nverse.sanitizer`

Reads the full HTTP request body from an `NVerseHttpRequestWrapper` and sanitizes it for safe log storage. Falls back to `Message.EXCEPTION_READING_REQUEST` on I/O error.

---

### `NVerseDomainValidated`

**Package:** `com.bloomscorp.nverse.validator`

Runtime annotation for controller classes that enforces domain and/or custom header validation via `NVerseValidationAspect`.

| Attribute | Default | Description |
|---|---|---|
| `headerKeys` | `{}` | Custom header names to validate. |
| `headerValues` | `{}` | Expected values for each `headerKey`; must be same length. |
| `origin` | `""` | When non-empty, chains a domain validation pass after custom header validation. |

---

### `NVerseValidationAspect`

**Package:** `com.bloomscorp.nverse.validator`

AspectJ `@Around` aspect that intercepts `@NVerseDomainValidated`-annotated controller methods and enforces origin/header validation before the method runs.

---

### `DomainValidator`

**Package:** `com.bloomscorp.nverse.validator`

Validates that the `Origin` header of a request is in the configured list of allowed domains.

| Method | Description |
|---|---|
| `validate(NVerseHttpRequestWrapper request)` | Returns `true` if the `Origin` header is present and in the allowed-domains list. |

---

### `NVerseValidator`

**Package:** `com.bloomscorp.nverse.validator`

Functional interface for request validators. Implement for custom validation rules.

---

### `NVerseTenantDAO`

**Package:** `com.bloomscorp.nverse.dao`

Persistence interface for tenant operations. Implement as a Spring `@Repository`.

| Method | Description |
|---|---|
| `verifyUniqueEmail(String email, boolean encoded)` | Returns `true` if no account with the email exists. |
| `verifyUniqueContactNumber(String contactNumber)` | Returns `true` if no account with the contact number exists. |
| `retrieveUserByEmail(String username)` | Returns the tenant matching the encrypted email, or `null`. |
| `addNewTenant(T user, R userRole)` | Persists tenant + initial role with default uniqueness checks. |
| `addNewTenant(T user, R userRole, boolean, boolean)` | Persists with explicit uniqueness check control. |

---

### `NVerseUserRoleDAO`

**Package:** `com.bloomscorp.nverse.dao`

Persistence interface for role operations.

| Method | Description |
|---|---|
| `addNewRole(R role)` | Persists a new role entity. Returns an `ActionCode` integer. |

---

### `NVerseRequest`

**Package:** `com.bloomscorp.nverse`

Immutable record for a username/password login request.

| Field | Description |
|---|---|
| `username` | Plaintext email address. |
| `password` | Plaintext password. |

---

### `NVerseResponse`

**Package:** `com.bloomscorp.nverse`

Immutable record for a successful login response.

| Field | Description |
|---|---|
| `jwt` | The signed compact JWT to return to the client. |

---

### `NVerseProviderResponse`

**Package:** `com.bloomscorp.nverse`

Immutable record for an auth-provider validation result.

| Field | Description |
|---|---|
| `success` | `true` if the provider matched. |
| `provider` | Ordinal of the `NVERSE_AUTH_PROVIDER` that was checked. |

---

### `NVerseAuthProviderValidationRequest`

**Package:** `com.bloomscorp.nverse`

Immutable record grouping a username and provider for auth-provider validation requests.

| Field | Description |
|---|---|
| `username` | Plaintext email address. |
| `provider` | The `NVERSE_AUTH_PROVIDER` to validate against. |

---

### `Constant`

**Package:** `com.bloomscorp.nverse.support`

Library-wide string constants: HTTP method names, header names, encoding labels, CORS constants, and the `Authorization: Bearer` prefix.

---

### `Message`

**Package:** `com.bloomscorp.nverse.support`

Human-readable error message strings used by `NVerseExceptionHandlerFilter` when mapping exceptions to JSON responses. Covers I/O failures, credential failures, JWT expiry/malformation, account state failures, and username-not-found scenarios.

---

## Package Structure

```
com.bloomscorp.nverse
│
├── NVerseAES.java                        AES-128 encryption/decryption via SHA digest key derivation
├── NVerseAuthenticationEntryPoint.java   Re-throws AuthenticationException to the exception handler filter
├── NVerseAuthenticationService.java      Login authentication and auth-provider validation utilities
├── NVerseAuthProviderValidationRequest.java  Record: username + provider validation request
├── NVerseAuthorityResolver.java          Resolves tenant from Authorization header value
├── NVerseCachedServletInputStream.java   Repeatable-read ServletInputStream backed by ByteArray
├── NVerseCORSConfigurationSource.java    Builds a CorsConfigurationSource for configured origins
├── NVerseEmailEncoder.java               AES email encryption/decryption with format validation
├── NVerseEmailValidator.java             Regex-based email format validator
├── NVerseExceptionHandlerFilter.java     Catches security exceptions → JSON failure responses
├── NVerseGatekeeper.java                 Abstract role-based authorization guard with surveillance pipeline
├── NVerseHttpRequestFilter.java          Wraps requests in NVerseHttpRequestWrapper
├── NVerseHttpRequestWrapper.java         HttpServletRequestWrapper with cached, re-readable body
├── NVerseJWTService.java                 JWT generation, extraction, and validation (HS512)
├── NVersePasswordEncoder.java            BCryptPasswordEncoder with pepper prepending
├── NVerseProviderResponse.java           Record: auth-provider validation result
├── NVerseRequest.java                    Record: username + password login request
├── NVerseRequestFilter.java              JWT filter — validates token, populates SecurityContext
├── NVerseResponse.java                   Record: JWT login success response
├── NVerseSecureLayer.java                Embedded tenant integrity check (id > 0, uid non-blank)
├── NVerseSecurityFilterChain.java        Assembles the Spring Security filter chain
├── NVerseSurveillanceReport.java         Record: surveillance pipeline result (failed + errorCode)
├── NVerseUser.java                       Spring Security User wrapper over NVerseTenant
├── NVerseUserDetailsService.java         UserDetailsService for encrypted-email lookup
├── NVerseValidatorUtilities.java         Static helper: isGenericNumericIDValid
│
├── dao/
│   ├── NVerseTenantDAO.java             Persistence interface for tenant CRUD operations
│   └── NVerseUserRoleDAO.java           Persistence interface for role persistence
│
├── pojo/
│   ├── NVERSE_AUTH_PROVIDER.java        Enum: UNKNOWN, BASIC, GOOGLE, FACEBOOK
│   ├── NVerseRole.java                  Interface: role entity contract (id + role enum)
│   └── NVerseTenant.java               Interface: tenant entity contract (full user model)
│
├── sanitizer/
│   ├── HttpRequestDumpSanitizer.java    Reads and sanitizes the HTTP request body dump
│   └── NVerseSanitizer.java            Abstract multi-layer XSS + OWASP HTML sanitizer
│
├── support/
│   ├── Constant.java                    Library-wide string constants
│   └── Message.java                    Human-readable authentication failure messages
│
└── validator/
    ├── DomainValidator.java             Origin header validator against an allowed-domains list
    ├── NVerseDomainValidated.java       Runtime annotation for domain/header validation
    ├── NVerseValidationAspect.java      AspectJ around-advice for NVerseDomainValidated
    └── NVerseValidator.java            Functional interface for pluggable validators
```

---

## Design Notes

**Generic type erasure and filter chain.** Because the security filter chain beans (`NVerseRequestFilter`, `NVerseExceptionHandlerFilter`) are generic, consuming applications must explicitly declare the generic type parameters when registering them as Spring beans. Using raw types will produce unchecked warnings and may cause class-cast exceptions at runtime.

**ECB mode is intentional for email.** AES/ECB produces deterministic ciphertext, which is required so that the same plaintext email always produces the same database lookup key. This is a deliberate trade-off: the database-level uniqueness constraint and the authentication lookup both rely on this determinism. For fields where indistinguishability matters (e.g., free-text), use a randomized cipher mode.

**Pepper rotation requires re-hashing.** The pepper is prepended to passwords before BCrypt hashing. If the pepper value changes, every existing hash becomes unverifiable. Plan pepper rotation carefully — it requires re-hashing all passwords in a single coordinated operation.

**`@NVerseDomainValidated` is class-level.** The AspectJ advice reads the annotation from the target class, not from the individual method. Placing the annotation only on a method without a class-level annotation will cause a `RuntimeException` at request time. Always annotate the class.

**bmx-alfred integration is required.** `NVerseExceptionHandlerFilter` takes a `CronManager` and logs every exception asynchronously. If you do not use bmx-alfred, you must provide a no-op `LogBook` and `CronManager` implementation to satisfy the generic constraints.

**No Spring dependency inside the security model itself.** `NVerseTenant`, `NVerseRole`, `NVerseGatekeeper`, and the sanitizer hierarchy have no Spring annotations — they are plain Java. Only the filter classes and the configuration helpers depend on Spring. This makes the domain model unit-testable without a Spring context.

---

## Dependencies Table

| Dependency | Version | Purpose |
|---|---|---|
| `spring-boot-starter-web` | 3.4.0 | Web MVC and embedded Tomcat |
| `spring-boot-starter-security` | 3.4.0 | Spring Security core |
| `spring-boot-starter-data-jpa` | 3.4.0 | JPA/Hibernate ORM |
| `jjwt-api` | 0.11.5 | JWT API |
| `jjwt-impl` | 0.11.5 | JWT implementation (runtime) |
| `jjwt-jackson` | 0.11.5 | JWT Jackson support |
| `owasp-java-html-sanitizer` | 20220608.1 | Safe HTML policy sanitization |
| `bmx-pastebox-java` | 0.0.3 | String utilities and reflection helpers |
| `bmx-hastar` | 0.0.3 | Exception types, ActionCode, ErrorCode, Message |
| `bmx-raintree` | 0.0.14 | JSON response envelope builder |
| `bmx-alfred` | 0.0.14 | Structured database-backed logging |
| `lombok` | 1.18.36 | Constructor/accessor annotation processing |
| `jetbrains:annotations` | 26.0.1 | `@NotNull`, `@Contract` nullability |

---

## License

bmx-nverse is released under the [MIT License](https://www.mit.edu/~amini/LICENSE.md).

Copyright © 2023 [Bloomscorp](https://github.com/bloomscorp)
