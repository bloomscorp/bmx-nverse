# BMX Hastar

**BMX Hastar** is a lightweight Java utility library for structured exception handling and standardized error/action messaging in Spring Boot applications. It provides a shared vocabulary — consistent exception types, integer operation codes, and human-readable message constants — that multiple services in an ecosystem can depend on to ensure uniform error handling behavior.

- **Group ID:** `com.bloomscorp`
- **Artifact ID:** `bmx-hastar`
- **Current Version:** `0.0.3`
- **Java:** 21+
- **License:** MIT
- **Published to:** [Maven Central](https://central.sonatype.com/artifact/com.bloomscorp/bmx-hastar)

---

## Table of Contents

- [Why BMX Hastar?](#why-bmx-hastar)
- [Installation](#installation)
- [Package Structure](#package-structure)
- [API Reference](#api-reference)
  - [CarrierException](#carrierexception)
  - [AuthorizationException](#authorizationexception)
  - [NoUserFoundException](#nouserfoundexception)
  - [ActionCode](#actioncode)
  - [ErrorCode](#errorcode)
  - [Message](#message)
  - [Constant](#constant)
- [Usage Guide](#usage-guide)
  - [Handling CRUD outcomes with ActionCode](#handling-crud-outcomes-with-actioncode)
  - [Propagating system errors with CarrierException and ErrorCode](#propagating-system-errors-with-carrierexception-and-errorcode)
  - [JWT filter failures with AuthorizationException](#jwt-filter-failures-with-authorizationexception)
  - [User lookup failures with NoUserFoundException](#user-lookup-failures-with-nouserfoundexception)
  - [Global exception handler with @ControllerAdvice](#global-exception-handler-with-controlleradvice)
  - [Using Message constants directly](#using-message-constants-directly)
- [Design Notes](#design-notes)
- [Dependencies](#dependencies)
- [License](#license)

---

## Why BMX Hastar?

In a multi-service Spring Boot ecosystem, each service tends to re-invent its own exception classes and error messages independently. This leads to inconsistent error responses, duplicated string literals scattered across codebases, and no shared contract between services about what a given error code means.

BMX Hastar solves this by providing:

- **Three purpose-built exception types** that cover the most common failure categories: generic system errors, authorization failures, and missing-user scenarios.
- **Two integer code registries** (`ActionCode` for CRUD outcomes, `ErrorCode` for system/domain errors) with a decode method on each that maps codes to ready-made messages.
- **One centralized message store** (`Message`) with ~30 pre-written, consistent user-facing strings — so the wording of error messages is decided once, not per-service.

The library has no Spring dependency of its own. It is pure Java, so it can be included in any Spring Boot service without pulling in a framework conflict.

---

## Installation

### Maven

```xml
<dependency>
    <groupId>com.bloomscorp</groupId>
    <artifactId>bmx-hastar</artifactId>
    <version>0.0.3</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
implementation("com.bloomscorp:bmx-hastar:0.0.3")
```

### Gradle (Groovy DSL)

```groovy
implementation 'com.bloomscorp:bmx-hastar:0.0.3'
```

---

## Package Structure

```
com.bloomscorp.hastar
│
├── CarrierException.java          # Generic wrapper exception with an origin and error code
├── AuthorizationException.java    # JWT / Spring Security authorization failure exception
├── NoUserFoundException.java      # Exception for failed username lookups
│
├── code/
│   ├── ActionCode.java            # Integer codes for CRUD outcomes + message resolver
│   └── ErrorCode.java             # Integer codes for system/domain errors + message decoder
│
└── support/
    ├── Message.java               # Centralized human-readable message string constants
    └── Constant.java              # Primitive library-wide fallback constants
```

---

## API Reference

### `CarrierException`

**Package:** `com.bloomscorp.hastar`
**Extends:** `RuntimeException`

A general-purpose wrapper exception that carries an original `Exception` alongside a structured `ErrorCode` integer. It is the primary exception type for propagating unexpected system-level failures (encoding errors, malformed payloads, forged requests, etc.) up through service layers to a global handler.

**Fields:**

| Field | Type | Description |
|---|---|---|
| `origin` | `Exception` | The original exception that triggered this carrier. Must not be `null`. |
| `errorCode` | `int` | A category code from `ErrorCode` identifying the type of failure. |

**Constructor:**

```java
new CarrierException(Exception origin, int errorCode)
```

Lombok generates `@AllArgsConstructor` and `@Getter` — both fields are constructor-injectable and readable via getters.

---

### `AuthorizationException`

**Package:** `com.bloomscorp.hastar`
**Extends:** `RuntimeException`

Represents a specific failure during the JWT authentication or Spring Security authorization process. Carries one of six error codes (`AECx01`–`AECx06`) and resolves the appropriate human-readable message via an overridden `getMessage()`.

**Error Code Constants:**

| Constant | Value | Meaning |
|---|---|---|
| `AECx01` | `901` | Some credentials were missing from the authentication request. |
| `AECx02` | `902` | The bearer token was absent from the request header. |
| `AECx03` | `903` | The username could not be resolved from the authentication request. |
| `AECx04` | `904` | The security context already holds a non-null authentication object. |
| `AECx05` | `905` | The provided JWT token is invalid or could not be verified. |
| `AECx06` | `906` | Reserved for future authorization failure scenarios. |

**Fields:**

| Field | Type | Description |
|---|---|---|
| `errorCode` | `int` | Must be one of `AECx01`–`AECx06`. |

**Constructor:**

```java
new AuthorizationException(int errorCode)
```

**Methods:**

| Method | Return | Description |
|---|---|---|
| `getMessage()` | `String` | Returns the human-readable message for the given error code. Falls back to a generic authorization exception message for unrecognized codes. Never returns `null`. |

---

### `NoUserFoundException`

**Package:** `com.bloomscorp.hastar`
**Extends:** `RuntimeException`

Thrown when a username lookup (typically inside a `UserDetailsService` implementation or repository layer) finds no associated account. The failed lookup key is preserved in the `username` field for logging and error reporting.

**Fields:**

| Field | Type | Description |
|---|---|---|
| `username` | `String` | The username that had no matching account. Must not be `null`. |

**Constructor:**

```java
new NoUserFoundException(String username)
```

---

### `ActionCode`

**Package:** `com.bloomscorp.hastar.code`

A non-instantiable utility class defining integer codes for CRUD operation outcomes, with a static `message()` resolver. By convention: positive codes indicate success, `0` means no action, and negative codes indicate failure.

**Constants:**

| Constant | Value | Meaning |
|---|---|---|
| `NO_ACTION` | `0` | No database action was attempted or required. |
| `INSERT_SUCCESS` | `1` | Insert operation succeeded. |
| `UPDATE_SUCCESS` | `2` | Update operation succeeded. |
| `DELETE_SUCCESS` | `3` | Delete operation succeeded. |
| `INSERT_FAILURE` | `-1` | Insert operation failed. |
| `NOT_UNIQUE` | `-2` | A uniqueness constraint violation prevented the operation. |
| `INCORRECT_INFORMATION` | `-3` | The request contained factually incorrect information. |
| `UNAUTHORIZED_ACCESS` | `-4` | The operation was rejected due to insufficient authorization. |
| `UPDATE_FAILURE` | `-5` | Update operation failed. |
| `PARTIAL_UPDATE_FAILURE` | `-6` | Some records in a batch update failed while others succeeded. |
| `DELETE_FAILURE` | `-7` | Delete operation failed. |

**Methods:**

| Method | Signature | Description |
|---|---|---|
| `message` | `static String message(int code)` | Resolves an action code to its human-readable message. Returns an empty string (`""`) for unrecognized codes. Never returns `null`. |

---

### `ErrorCode`

**Package:** `com.bloomscorp.hastar.code`

A non-instantiable utility class defining negative integer codes for system-level and domain-level error categories, with a static `decode()` resolver. All codes are negative to distinguish them from `ActionCode` success states.

**Constants:**

| Constant | Value | Meaning |
|---|---|---|
| `GENERIC` | `-1` | Generic fallback for unclassified failures. |
| `EMAIL_ENCODER` | `-2` | Failure during email address encoding or decoding. |
| `PASSWORD_ENCODER` | `-3` | Failure during password encoding or decoding. |
| `MALFORMED_PAYLOAD` | `-4` | The request payload is structurally malformed. |
| `FORGED_REQUEST` | `-5` | The request appears to have been forged or tampered with. |
| `AUTHORIZATION_DENIED` | `-6` | Authorization for the request was explicitly denied. |
| `INVALID_REQUEST` | `-7` | The request itself is considered invalid. |
| `INVALID_INFORMATION` | `-8` | The request contains semantically incorrect information. |

> **Note:** `MALFORMED_PAYLOAD` and `FORGED_REQUEST` intentionally decode to the same message (`Message.MALFORMED_INFORMATION`). From a consumer's perspective, both represent integrity violations, and leaking the distinction could expose internal security logic.

**Methods:**

| Method | Signature | Description |
|---|---|---|
| `decode` | `static String decode(int code)` | Decodes an error code into its human-readable message. Falls back to `Message.GENERIC_ERROR` for unrecognized codes. Never returns `null`. |

---

### `Message`

**Package:** `com.bloomscorp.hastar.support`

A non-instantiable utility class holding all human-readable message string constants used across the library and intended for use in consuming applications. Grouped by domain below for clarity.

**Authentication & Credentials:**

| Constant | Value |
|---|---|
| `IO_FAILURE` | `"An I/O failure was encountered."` |
| `SERVLET_FAILURE` | `"A servlet failed while trying to process your request."` |
| `EXPIRED_CREDENTIALS` | `"The credentials used have expired."` |
| `ILLEGAL_ARGUMENTS` | `"Illegal credentials were used to authenticate."` |
| `BAD_CREDENTIALS` | `"The credentials provided were incorrect."` |
| `ACCOUNT_DISABLED` | `"The associated account is disabled and not accessible."` |
| `AUTH_INFO_UNAVAILABLE` | `"No associated account was found. Bad credentials, maybe."` |
| `INTERNAL_AUTHENTICATION_SERVICE_FAILURE` | `"Our system failed to authenticate the credentials."` |
| `MALFORMED_CREDENTIALS` | `"The credentials have been tampered with."` |
| `USERNAME_NOT_FOUND` | `"The username does not have any associated account."` |
| `USER_ACCOUNT_LOCKED` | `"The user account may be locked. Contact systems administrator."` |

**Request Validation:**

| Constant | Value |
|---|---|
| `INCOMPLETE_INFORMATION` | `"Complete information was not provided."` |
| `MALFORMED_INFORMATION` | `"The information provided is malformed and cannot be accepted."` |
| `INVALID_REQUEST` | `"This request is invalid."` |
| `INCORRECT_INFORMATION` | `"The request contains incorrect information."` |
| `NOT_UNIQUE` | `"The request is not unique."` |
| `AUTHORIZATION_DENIED` | `"Authorization has been denied."` |

**System Errors:**

| Constant | Value |
|---|---|
| `UNHANDLED_EXCEPTION` | `"The system detected an unknown error."` |
| `EXCEPTION_READING_REQUEST` | `"An exception occurred while trying to read the request dump contents."` |
| `GENERIC_ERROR` | `"An error occurred."` |
| `EMAIL_ENCODER_ERROR` | `"An error occurred while trying to encrypt or decrypt an email address"` |
| `PASSWORD_ENCODER_ERROR` | `"An error occurred while trying to encrypt or decrypt a password"` |

**CRUD Operation Outcomes:**

| Constant | Value |
|---|---|
| `INSERT_SUCCESS` | `"The request is successfully added to database."` |
| `UPDATE_SUCCESS` | `"The request is successfully updated to database."` |
| `DELETE_SUCCESS` | `"The request is successfully deleted from database."` |
| `INSERT_FAILURE` | `"The request failed to add to database."` |
| `UPDATE_FAILURE` | `"The request failed to update the database."` |
| `PARTIAL_UPDATE_FAILURE` | `"Some instances failed to update the database while others were successful."` |
| `DELETE_FAILURE` | `"The request failed to delete from database."` |

---

### `Constant`

**Package:** `com.bloomscorp.hastar.support`

A non-instantiable utility class holding primitive library-wide sentinel values.

| Constant | Value | Usage |
|---|---|---|
| `BLANK_STRING_VALUE` | `""` | Default fallback returned by `ActionCode.message()` when an unrecognized code is passed. |

---

## Usage Guide

### Handling CRUD outcomes with ActionCode

Use `ActionCode` constants as return values from your service or repository layer to represent the outcome of a database operation. Pair with `ActionCode.message()` to get the corresponding string for the API response.

```java
// In a service class
public int saveUser(UserDto dto) {
    try {
        userRepository.save(toEntity(dto));
        return ActionCode.INSERT_SUCCESS;
    } catch (DataIntegrityViolationException e) {
        return ActionCode.NOT_UNIQUE;
    } catch (Exception e) {
        return ActionCode.INSERT_FAILURE;
    }
}

// In a controller or response builder
int result = userService.saveUser(dto);

ApiResponse response = ApiResponse.builder()
    .code(result)
    .message(ActionCode.message(result))
    .build();
```

---

### Propagating system errors with CarrierException and ErrorCode

Use `CarrierException` to wrap low-level exceptions at the boundary where they are caught, attaching an `ErrorCode` so the global handler can interpret them without inspecting the original exception type.

```java
// In a service class
public String encodeUserEmail(String email) {
    try {
        return emailEncoder.encode(email);
    } catch (Exception e) {
        throw new CarrierException(e, ErrorCode.EMAIL_ENCODER);
    }
}

// In a @ControllerAdvice
@ExceptionHandler(CarrierException.class)
public ResponseEntity<ApiResponse> handleCarrierException(CarrierException ex) {
    log.error("System error [code={}]: {}", ex.getErrorCode(), ex.getOrigin().getMessage());

    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error(ErrorCode.decode(ex.getErrorCode())));
}
```

---

### JWT filter failures with AuthorizationException

Throw `AuthorizationException` from a JWT filter when a specific precondition is not met. The `getMessage()` override handles translating the code to a message automatically.

```java
// In a JWT authentication filter
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain chain) throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new AuthorizationException(AuthorizationException.AECx02);
    }

    String token = authHeader.substring(7);

    if (!jwtService.isValid(token)) {
        throw new AuthorizationException(AuthorizationException.AECx05);
    }

    String username = jwtService.extractUsername(token);

    if (username == null) {
        throw new AuthorizationException(AuthorizationException.AECx03);
    }

    // proceed...
    chain.doFilter(request, response);
}

// In a @ControllerAdvice or AuthenticationEntryPoint
@ExceptionHandler(AuthorizationException.class)
public ResponseEntity<ApiResponse> handleAuthorizationException(AuthorizationException ex) {
    // getMessage() resolves automatically from the error code
    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(ex.getMessage()));
}
```

---

### User lookup failures with NoUserFoundException

Throw `NoUserFoundException` from a `UserDetailsService` implementation when no account matches the supplied username. The username is captured for logging.

```java
// In a UserDetailsService implementation
@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return userRepository.findByUsername(username)
        .map(UserPrincipal::from)
        .orElseThrow(() -> new NoUserFoundException(username));
}

// In a @ControllerAdvice
@ExceptionHandler(NoUserFoundException.class)
public ResponseEntity<ApiResponse> handleNoUserFound(NoUserFoundException ex) {
    log.warn("Login attempt for unknown username: {}", ex.getUsername());

    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(Message.USERNAME_NOT_FOUND));
}
```

---

### Global exception handler with @ControllerAdvice

A complete `@ControllerAdvice` that handles all three exception types from this library in one place:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CarrierException.class)
    public ResponseEntity<ApiResponse> onCarrierException(CarrierException ex) {
        log.error("CarrierException [code={}]", ex.getErrorCode(), ex.getOrigin());
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponse(ex.getErrorCode(), ErrorCode.decode(ex.getErrorCode())));
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ApiResponse> onAuthorizationException(AuthorizationException ex) {
        log.warn("AuthorizationException [code={}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ApiResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(NoUserFoundException.class)
    public ResponseEntity<ApiResponse> onNoUserFound(NoUserFoundException ex) {
        log.warn("No account found for username: {}", ex.getUsername());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ApiResponse(ActionCode.UNAUTHORIZED_ACCESS, Message.USERNAME_NOT_FOUND));
    }
}
```

---

### Using Message constants directly

`Message` constants can also be used directly anywhere in a consuming application — in log statements, response builders, or validation utilities — to maintain wording consistency across services.

```java
// In a validation utility
if (dto.getEmail() == null || dto.getPassword() == null) {
    return ApiResponse.failure(ActionCode.INCORRECT_INFORMATION, Message.INCOMPLETE_INFORMATION);
}

// In a Spring Security AuthenticationFailureHandler
@Override
public void onAuthenticationFailure(HttpServletRequest request,
                                    HttpServletResponse response,
                                    AuthenticationException exception) throws IOException {
    String msg;

    if (exception instanceof BadCredentialsException) {
        msg = Message.BAD_CREDENTIALS;
    } else if (exception instanceof DisabledException) {
        msg = Message.ACCOUNT_DISABLED;
    } else if (exception instanceof LockedException) {
        msg = Message.USER_ACCOUNT_LOCKED;
    } else {
        msg = Message.INTERNAL_AUTHENTICATION_SERVICE_FAILURE;
    }

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.getWriter().write(msg);
}
```

---

## Design Notes

**No Spring dependency.** The library is intentionally pure Java. It has no compile-time dependency on `spring-boot`, `spring-security`, or `spring-web`. This avoids version conflicts and makes the library consumable by any Java 21 project.

**Non-instantiable utility classes.** `Message`, `Constant`, `ActionCode`, and `ErrorCode` all have private constructors. They are pure static namespaces — they hold no state and are not designed to be extended or injected.

**Code sign convention.** `ActionCode` uses a deliberate sign convention: `> 0` is success, `0` is no-op, `< 0` is failure. `ErrorCode` is always negative. This means you can safely combine both in a single integer field in an API response DTO without collision, as long as you document which class the code belongs to.

**`MALFORMED_PAYLOAD` and `FORGED_REQUEST` share a message.** This is intentional. Distinguishing between a malformed payload and a forged/tampered request in a user-visible message would expose internal security logic. Both decode to `Message.MALFORMED_INFORMATION` for the consumer.

**Lombok usage.** `CarrierException`, `AuthorizationException`, and `NoUserFoundException` use Lombok's `@Getter` and `@AllArgsConstructor`. If your project does not have Lombok configured, ensure the annotation processor is set up in your build tool.

---

## Dependencies

| Dependency | Version | Scope |
|---|---|---|
| `org.projectlombok:lombok` | `1.18.36` | Optional (annotation processor) |
| `org.jetbrains:annotations` | `26.0.1` | Compile |

---

## License

BMX Hastar is released under the [MIT License](https://www.mit.edu/~amini/LICENSE.md).

Copyright © 2023 [Bloomscorp](https://github.com/bloomscorp)
