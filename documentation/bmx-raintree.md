# BMX RainTree

**BMX RainTree** is a lightweight Java library that enforces a standardized JSON response envelope — the **RainTree protocol** — for RESTful APIs built in Spring Boot. Every response produced by this library carries a guaranteed `success` flag and `message` string at the JSON root, with optional typed payload fields. The library provides three tiers of response construction, from simple one-liners to fully typed abstract response builders.

- **Group ID:** `com.bloomscorp`
- **Artifact ID:** `bmx-raintree`
- **Current Version:** `0.0.14`
- **Java:** 21+
- **License:** MIT
- **Published to:** [Maven Central](https://central.sonatype.com/artifact/com.bloomscorp/bmx-raintree)

---

## Table of Contents

- [Why BMX RainTree?](#why-bmx-raintree)
- [Installation](#installation)
- [Package Structure](#package-structure)
- [The RainTree Protocol](#the-raintree-protocol)
- [API Reference](#api-reference)
  - [RainTree](#raintree)
  - [RainTreeResponse](#raintreeresponse)
  - [RainEntity](#rainentitye)
  - [RainFailedEntity](#rainfailedentitye)
  - [RainResponse](#rainresponsee)
  - [RainEnhancedResponse](#rainenhancedresponsee-p)
  - [GsonExclude](#gsonexclude)
  - [GsonExclusionStrategy](#gsonexclusionstrategy)
  - [Constant](#constant)
- [Usage Guide](#usage-guide)
  - [Simple responses with RainTree](#simple-responses-with-raintree)
  - [Parameterized responses](#parameterized-responses)
  - [Handling CRUD outcomes with ActionCode](#handling-crud-outcomes-with-actioncode)
  - [Typed entity responses with RainEntity](#typed-entity-responses-with-rainentity)
  - [Typed failure responses with RainFailedEntity](#typed-failure-responses-with-rainfailedentity)
  - [Abstract response builders with RainResponse](#abstract-response-builders-with-rainresponse)
  - [Parameterized builders with RainEnhancedResponse](#parameterized-builders-with-rainenhancedresponse)
  - [Excluding fields from serialization](#excluding-fields-from-serialization)
- [Response Format Reference](#response-format-reference)
- [Design Notes](#design-notes)
- [Dependencies](#dependencies)
- [License](#license)

---

## Why BMX RainTree?

In a multi-service Spring Boot ecosystem, each controller tends to return its own ad-hoc JSON structure. One endpoint returns `{"ok": true}`, another returns `{"status": "success"}`, a third returns the raw entity with no envelope at all. Consumers cannot write generic error handling — every integration needs its own parsing logic.

BMX RainTree solves this by providing:

- **A fixed protocol contract** — every response, regardless of endpoint or service, always contains `"success"` and `"message"` at the root. Consumers can always check these two fields before inspecting any payload.
- **Three tiers of response construction** — flat JSON strings for simple cases, typed POJO wrappers for entity-carrying responses, and abstract builder classes for domain-specific response classes.
- **Gson-based serialization with field exclusion** — a shared, pre-configured Gson instance handles all serialization, and the `@GsonExclude` annotation lets specific fields be silently omitted from output without changing the domain model.
- **Seamless ActionCode integration** — pairs with [bmx-hastar](https://github.com/bloomscorp/bmx-hastar) so CRUD operation outcomes map directly to typed, pre-worded responses without string literals scattered across the codebase.

---

## Installation

### Maven

```xml
<dependency>
    <groupId>com.bloomscorp</groupId>
    <artifactId>bmx-raintree</artifactId>
    <version>0.0.14</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
implementation("com.bloomscorp:bmx-raintree:0.0.14")
```

### Gradle (Groovy DSL)

```groovy
implementation 'com.bloomscorp:bmx-raintree:0.0.14'
```

---

## Package Structure

```
com.bloomscorp.raintree
│
├── RainTree.java                    # Core response engine — produces all JSON strings
├── RainTreeResponse.java            # Base response POJO with success + message
│
├── configuration/
│   ├── GsonExclude.java            # Field-level annotation to exclude from serialization
│   └── GsonExclusionStrategy.java  # Gson ExclusionStrategy that enforces @GsonExclude
│
├── restful/
│   ├── RainEntity.java             # Typed response envelope for a single entity payload
│   ├── RainFailedEntity.java       # Typed failure envelope — always success=false, entity=null
│   ├── RainResponse.java           # Abstract base for domain-specific response builders
│   └── RainEnhancedResponse.java   # Abstract extension for parameterized response builders
│
└── support/
    └── Constant.java               # String constants for JSON keys and default messages
```

---

## The RainTree Protocol

Every JSON response produced or returned by this library adheres to the following contract:

1. **Always has `"success"`** — a `boolean` at the JSON root. `true` for success, `false` for failure.
2. **Always has `"message"`** — a `String` at the JSON root. Human-readable outcome description.
3. **Optionally has payload fields** — either merged at the root as named keys, or in a typed `"entity"` field, depending on the response format used.

The canonical response shapes are:

**Shape 1 — Simple:**
```json
{
  "success": true,
  "message": "The request was successful."
}
```

**Shape 2 — Parameterized (list under a named key):**
```json
{
  "success": true,
  "message": "",
  "users": [
    { "id": 1, "name": "Alice" },
    { "id": 2, "name": "Bob" }
  ]
}
```

**Shape 3 — Parameterized (single entity under a named key):**
```json
{
  "success": true,
  "message": "",
  "user": { "id": 1, "name": "Alice" }
}
```

**Shape 4 — Typed entity (via `RainEntity` serialization):**
```json
{
  "success": true,
  "message": null,
  "entity": { "id": 1, "name": "Alice" }
}
```

**Shape 5 — Typed failure (via `RainFailedEntity` serialization):**
```json
{
  "success": false,
  "message": "The username does not have any associated account.",
  "entity": null
}
```

---

## API Reference

### `RainTree`

**Package:** `com.bloomscorp.raintree`

The core engine. Holds a `private static final` thread-safe Gson instance configured with `GsonExclusionStrategy`. All JSON production in the library ultimately flows through one of its methods. Safe to register as a singleton Spring bean.

**Methods:**

| Method | Return | Description |
|---|---|---|
| `renderResponse(Object object)` | `String` | Serializes any Java object to JSON. No envelope is injected. |
| `renderResponse(boolean success, String message)` | `String` | Produces `{"success": ..., "message": "..."}`. |
| `renderResponse(boolean success, String message, HashMap<String, Object> parameters)` | `String` | Standard envelope with additional fields merged at the JSON root. `parameters` may be `null`. |
| `successResponse()` | `String` | `{"success": true, "message": "The request was successful."}` |
| `successResponse(String message)` | `String` | `{"success": true, "message": "..."}` |
| `failureResponse()` | `String` | `{"success": false, "message": "The request failed."}` |
| `failureResponse(String message)` | `String` | `{"success": false, "message": "..."}` |
| `failureResponse(int actionCode)` | `String` | `{"success": false, "message": "<ActionCode resolved message>"}` |
| `renderParameterizedSuccessResponse(HashMap<String, Object> parameters)` | `String` | `{"success": true, "message": "", ...parameters}` |

---

### `RainTreeResponse`

**Package:** `com.bloomscorp.raintree`

The base POJO that represents the protocol's minimum response shape. Used as a return type wherever a simple success/failure object is needed — for example, from `RainResponse.prepareActionResponse`.

**Fields:**

| Field | Type | Description |
|---|---|---|
| `success` | `boolean` | `true` if the operation succeeded, `false` otherwise. |
| `message` | `String` | Human-readable outcome description. |

**Constructors:** No-args (`@NoArgsConstructor`) and all-args (`@AllArgsConstructor`) generated by Lombok.

---

### `RainEntity<E>`

**Package:** `com.bloomscorp.raintree.restful`
**Extends:** `RainTreeResponse`

A typed envelope that carries a single domain entity. The `success` field is automatically derived from the entity's nullability — a non-null entity means success, a null entity means failure.

**Fields:**

| Field | Type | Description |
|---|---|---|
| `success` | `boolean` | (inherited) Auto-set to `entity != null` by the single-arg constructor. |
| `message` | `String` | (inherited) Not populated by this class; defaults to `null`. |
| `entity` | `E` | The domain entity payload. `null` if the operation found nothing. |

**Constructors:**

| Constructor | Behavior |
|---|---|
| `RainEntity()` | No-args. All fields remain at their Java defaults. |
| `RainEntity(E entity)` | Sets `this.entity = entity` and `this.success = entity != null`. |

---

### `RainFailedEntity<E>`

**Package:** `com.bloomscorp.raintree.restful`
**Extends:** `RainEntity<E>`

A fixed-failure envelope. `success` and `entity` are hardwired to `false` and `null` via field initializers. Use this when you need to return a typed `RainEntity`-shaped response that unambiguously signals failure, including the reason why.

**Fields:**

| Field | Type | Value | Description |
|---|---|---|---|
| `success` | `boolean` | Always `false` | Shadows the parent's `success` field via initializer. |
| `entity` | `E` | Always `null` | Shadows the parent's `entity` field via initializer. No entity is available. |
| `message` | `String` | Set by constructor | Human-readable failure explanation. Typically sourced from `com.bloomscorp.hastar.support.Message`. |

**Constructor:**

```java
new RainFailedEntity<>(String message)
```

---

### `RainResponse<E>`

**Package:** `com.bloomscorp.raintree.restful`

Abstract base class for domain-specific response builders. Subclasses implement `buildEntity` and `buildList` to define the JSON key names under which payloads are nested. Holds a `RainTree` instance injected via the constructor.

**Constructor:**

```java
// Typically called as super(rainTree) from a subclass
new RainResponse<E>(RainTree rainTree)
```

**Methods:**

| Method | Modifier | Return | Description |
|---|---|---|---|
| `prepareActionResponse(int action)` | `public static` | `RainTreeResponse` | Maps an `ActionCode` integer to a `RainTreeResponse` with the correct `success` and `message`. |
| `prepareList(List<? extends T> list, String responseParameter)` | `protected` | `String` | Produces a parameterized success JSON with the list nested under `responseParameter`. |
| `prepareEntity(T object, String responseParameter)` | `protected` | `String` | Produces a parameterized success JSON with the object nested under `responseParameter`. |
| `buildEntity(E entity)` | `public abstract` | `String` | Subclasses define the entity key name and delegate to `prepareEntity`. |
| `buildList(List<E> list)` | `public abstract` | `String` | Subclasses define the list key name and delegate to `prepareList`. |

**`prepareActionResponse` success mapping:**

| ActionCode constant | Value | `success` result |
|---|---|---|
| `INSERT_SUCCESS` | `1` | `true` |
| `UPDATE_SUCCESS` | `2` | `true` |
| `DELETE_SUCCESS` | `3` | `true` |
| Any other code | — | `false` |

---

### `RainEnhancedResponse<E, P>`

**Package:** `com.bloomscorp.raintree.restful`
**Extends:** `RainResponse<E>`

Extends `RainResponse` with a third abstract method for cases where building a response requires a runtime parameter of type `P` beyond the entity type `E` alone — for example, a filter criterion, a request-time identifier, or a context object.

**Constructor:**

```java
new RainEnhancedResponse<E, P>(RainTree rainTree)
```

**Methods:**

| Method | Modifier | Return | Description |
|---|---|---|---|
| `prepareResponse(P parameter)` | `public abstract` | `RainTreeResponse` | Subclasses implement this for response logic that requires a runtime parameter not expressible through `buildEntity` or `buildList`. |
| *(all of `RainResponse<E>`)* | | | All inherited methods and abstract methods from the parent are also present. |

---

### `@GsonExclude`

**Package:** `com.bloomscorp.raintree.configuration`
**Target:** `ElementType.FIELD`
**Retention:** `RetentionPolicy.RUNTIME`

A marker annotation. Any field annotated with `@GsonExclude` is silently omitted from all JSON output produced by `RainTree`'s Gson instance. Has no effect on Jackson-based serialization.

---

### `GsonExclusionStrategy`

**Package:** `com.bloomscorp.raintree.configuration`
**Implements:** `com.google.gson.ExclusionStrategy`

Implements Gson's `ExclusionStrategy` to skip fields annotated with `@GsonExclude`. Registered on the static Gson instance inside `RainTree`. Class-level exclusion is never applied — filtering is strictly field-level.

---

### `Constant`

**Package:** `com.bloomscorp.raintree.support`

Non-instantiable utility class holding all string constants used by the library internally.

| Constant | Value |
|---|---|
| `BLANK_STRING_VALUE` | `""` |
| `JSON_RESPONSE_SUCCESS` | `"success"` |
| `JSON_RESPONSE_MESSAGE` | `"message"` |
| `JSON_RESPONSE_GENERIC_SUCCESS` | `"The request was successful."` |
| `JSON_RESPONSE_GENERIC_FAILURE` | `"The request failed."` |

---

## Usage Guide

### Simple responses with RainTree

Register `RainTree` as a Spring bean and inject it wherever a controller or service needs to return a response string.

```java
@Configuration
public class RainTreeConfig {

    @Bean
    public RainTree rainTree() {
        return new RainTree();
    }
}
```

```java
@RestController
@RequestMapping("/api/ping")
public class PingController {

    private final RainTree rainTree;

    public PingController(RainTree rainTree) {
        this.rainTree = rainTree;
    }

    @GetMapping
    public String ping() {
        return this.rainTree.successResponse();
        // → {"success":true,"message":"The request was successful."}
    }

    @GetMapping("/fail")
    public String forceFail() {
        return this.rainTree.failureResponse("Service is temporarily unavailable.");
        // → {"success":false,"message":"Service is temporarily unavailable."}
    }
}
```

---

### Parameterized responses

Use `renderParameterizedSuccessResponse` when the response needs to carry additional fields alongside the standard envelope, without defining a full typed class:

```java
@GetMapping("/stats")
public String getStats() {
    HashMap<String, Object> params = new HashMap<>();
    params.put("totalUsers", userRepository.count());
    params.put("activeUsers", userRepository.countByActive(true));
    return this.rainTree.renderParameterizedSuccessResponse(params);
    // → {"success":true,"message":"","totalUsers":1042,"activeUsers":891}
}
```

---

### Handling CRUD outcomes with ActionCode

Pair `RainTree` with [bmx-hastar](https://github.com/bloomscorp/bmx-hastar)'s `ActionCode` to translate integer service-layer outcomes into well-worded responses without hardcoded strings:

```java
// In a service class
public int createUser(UserDto dto) {
    try {
        this.userRepository.save(toEntity(dto));
        return ActionCode.INSERT_SUCCESS;
    } catch (DataIntegrityViolationException e) {
        return ActionCode.NOT_UNIQUE;
    } catch (Exception e) {
        return ActionCode.INSERT_FAILURE;
    }
}
```

```java
// In a controller — string-based approach
@PostMapping("/users")
public String createUser(@RequestBody UserDto dto) {
    int result = this.userService.createUser(dto);

    if (result == ActionCode.INSERT_SUCCESS)
        return this.rainTree.successResponse(ActionCode.message(result));

    return this.rainTree.failureResponse(result);
    // INSERT_FAILURE → {"success":false,"message":"The request failed to add to database."}
    // NOT_UNIQUE     → {"success":false,"message":"The request is not unique."}
}
```

```java
// In a controller — POJO-based approach using prepareActionResponse
@PostMapping("/users")
public RainTreeResponse createUser(@RequestBody UserDto dto) {
    int result = this.userService.createUser(dto);
    return RainResponse.prepareActionResponse(result);
    // INSERT_SUCCESS → {"success":true,"message":"The request is successfully added to database."}
    // INSERT_FAILURE → {"success":false,"message":"The request failed to add to database."}
}
```

---

### Typed entity responses with RainEntity

Use `RainEntity<E>` when a controller returns a single domain object and `success` should be derived automatically from whether the entity exists:

```java
@GetMapping("/users/{id}")
public RainEntity<User> getUser(@PathVariable Long id) {
    User user = this.userRepository.findById(id).orElse(null);
    return new RainEntity<>(user);
    // found     → {"success":true,"message":null,"entity":{"id":1,"name":"Alice"}}
    // not found → {"success":false,"message":null,"entity":null}
}
```

If a `String` response is needed instead of a POJO, serialize through `RainTree`:

```java
@GetMapping("/users/{id}")
public String getUser(@PathVariable Long id) {
    User user = this.userRepository.findById(id).orElse(null);
    return this.rainTree.renderResponse(new RainEntity<>(user));
}
```

---

### Typed failure responses with RainFailedEntity

Use `RainFailedEntity<E>` when the caller always expects a `RainEntity`-shaped envelope but the operation failed for a specific, communicable reason:

```java
@GetMapping("/users/{id}")
public RainEntity<User> getUser(@PathVariable Long id, Principal principal) {

    if (!this.authService.canViewUser(principal, id))
        return new RainFailedEntity<>(Message.AUTHORIZATION_DENIED);
        // → {"success":false,"message":"Authorization has been denied.","entity":null}

    User user = this.userRepository.findById(id).orElse(null);

    if (user == null)
        return new RainFailedEntity<>(Message.USERNAME_NOT_FOUND);
        // → {"success":false,"message":"The username does not have any associated account.","entity":null}

    return new RainEntity<>(user);
    // → {"success":true,"message":null,"entity":{"id":1,"name":"Alice"}}
}
```

---

### Abstract response builders with RainResponse

For a domain resource that frequently returns lists or entities under a consistent key name, extend `RainResponse<E>` once and reuse across all controllers that work with that resource:

```java
@Component
public class UserRainResponse extends RainResponse<User> {

    public UserRainResponse(RainTree rainTree) {
        super(rainTree);
    }

    @Override
    public String buildEntity(User user) {
        return this.prepareEntity(user, "user");
        // → {"success":true,"message":"","user":{"id":1,"name":"Alice"}}
    }

    @Override
    public String buildList(List<User> users) {
        return this.prepareList(users, "users");
        // → {"success":true,"message":"","users":[{"id":1,"name":"Alice"},{"id":2,"name":"Bob"}]}
    }
}
```

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserRainResponse userRainResponse;

    public UserController(UserService userService, UserRainResponse userRainResponse) {
        this.userService = userService;
        this.userRainResponse = userRainResponse;
    }

    @GetMapping
    public String getAllUsers() {
        return this.userRainResponse.buildList(this.userService.findAll());
    }

    @GetMapping("/{id}")
    public String getUser(@PathVariable Long id) {
        return this.userRainResponse.buildEntity(this.userService.findById(id));
    }

    @PostMapping
    public RainTreeResponse createUser(@RequestBody UserDto dto) {
        int result = this.userService.create(dto);
        return UserRainResponse.prepareActionResponse(result);
    }

    @PutMapping("/{id}")
    public RainTreeResponse updateUser(@PathVariable Long id, @RequestBody UserDto dto) {
        int result = this.userService.update(id, dto);
        return UserRainResponse.prepareActionResponse(result);
    }

    @DeleteMapping("/{id}")
    public RainTreeResponse deleteUser(@PathVariable Long id) {
        int result = this.userService.delete(id);
        return UserRainResponse.prepareActionResponse(result);
    }
}
```

---

### Parameterized builders with RainEnhancedResponse

Extend `RainEnhancedResponse<E, P>` when response construction requires a runtime parameter that is not the entity itself — such as a search term, a filter object, or a configuration value:

```java
@Component
public class UserSearchRainResponse extends RainEnhancedResponse<User, String> {

    private final UserService userService;

    public UserSearchRainResponse(RainTree rainTree, UserService userService) {
        super(rainTree);
        this.userService = userService;
    }

    @Override
    public String buildEntity(User user) {
        return this.prepareEntity(user, "user");
    }

    @Override
    public String buildList(List<User> users) {
        return this.prepareList(users, "users");
    }

    @Override
    public RainTreeResponse prepareResponse(String searchTerm) {
        List<User> results = this.userService.search(searchTerm);
        boolean found = !results.isEmpty();
        return new RainTreeResponse(
            found,
            found ? Message.INSERT_SUCCESS : Message.INCOMPLETE_INFORMATION
        );
    }
}
```

```java
@GetMapping("/search")
public RainTreeResponse search(@RequestParam String q) {
    return this.userSearchRainResponse.prepareResponse(q);
}
```

---

### Excluding fields from serialization

Annotate any field with `@GsonExclude` to prevent it from appearing in JSON output produced by `RainTree`:

```java
public class User {

    public Long id;
    public String name;
    public String email;

    @GsonExclude
    public String passwordHash;

    @GsonExclude
    public String internalAuditToken;
}
```

```java
this.rainTree.renderResponse(new RainEntity<>(user));
// → {"success":true,"message":null,"entity":{"id":1,"name":"Alice","email":"alice@example.com"}}
// passwordHash and internalAuditToken are absent from the output
```

> **Note:** `@GsonExclude` only applies when serialization goes through `RainTree`'s Gson instance — i.e., when you call `rainTree.renderResponse(object)` or return a JSON string from the `RainTree` methods. If a `RainEntity` or `RainFailedEntity` is returned directly as a `@RestController` response body without going through `renderResponse`, Spring will serialize it using Jackson. In that path, `@GsonExclude` has no effect — use `@JsonIgnore` for Jackson-based exclusion.

---

## Response Format Reference

| Source | `success` | `message` | Payload shape |
|---|---|---|---|
| `successResponse()` | `true` | `"The request was successful."` | none |
| `successResponse(msg)` | `true` | custom | none |
| `failureResponse()` | `false` | `"The request failed."` | none |
| `failureResponse(msg)` | `false` | custom | none |
| `failureResponse(actionCode)` | `false` | resolved from `ActionCode` | none |
| `renderResponse(success, message)` | custom | custom | none |
| `renderParameterizedSuccessResponse(params)` | `true` | `""` | params merged at root |
| `new RainEntity<>(entity)` — entity non-null | `true` | `null` | `"entity": <E>` |
| `new RainEntity<>(null)` | `false` | `null` | `"entity": null` |
| `new RainFailedEntity<>(msg)` | `false` | custom | `"entity": null` |
| `prepareActionResponse(INSERT/UPDATE/DELETE_SUCCESS)` | `true` | resolved from `ActionCode` | none |
| `prepareActionResponse(any other code)` | `false` | resolved from `ActionCode` | none |
| `buildEntity(entity)` | `true` | `""` | `"<key>": <E>` at root |
| `buildList(list)` | `true` | `""` | `"<key>": [<E>]` at root |

---

## Design Notes

**Gson, not Jackson.** The library uses Gson for all serialization rather than Spring's default Jackson. This gives the library full control over serialization behavior regardless of how the consuming application configures its `ObjectMapper`. The trade-off is that `@JsonIgnore` and other Jackson annotations have no effect inside `RainTree`; use `@GsonExclude` instead.

**`@GsonExclude` only covers `RainTree` output.** When a `RainEntity` or `RainFailedEntity` is returned directly as a `@RestController` response body — without going through `renderResponse` — Spring uses Jackson to serialize it. `@GsonExclude` does not apply in that path.

**`RainFailedEntity` uses field hiding, not constructor logic.** The `success = false` and `entity = null` values in `RainFailedEntity` are set as field initializers on the class itself, not inside the constructor. They shadow the same-named fields from `RainTreeResponse` and `RainEntity`. Gson serializes based on the declared fields of the runtime class, so the overrides take effect correctly in JSON output.

**`RainEntity.message` is intentionally `null` by default.** The single-arg constructor of `RainEntity` only sets `entity` and derives `success`. The `message` field inherited from `RainTreeResponse` is left at its Java default (`null`). Entity-carrying responses are not expected to carry a message — consumers should check `success` and `entity`, not `message`, when working with `RainEntity` or `RainFailedEntity` directly.

**Thread safety.** The `Gson` instance inside `RainTree` is `private static final` and constructed once at class load time. Gson instances are thread-safe for concurrent read operations, so `RainTree` is safe to register as a singleton Spring bean without any synchronization.

**No Spring dependency in the library itself.** The library has no compile-time dependency on `spring-boot`, `spring-web`, or `spring-security`. It is pure Java 21 and can be included in any Java project without risk of framework version conflicts.

**`ActionCode` sign convention.** ActionCode values from bmx-hastar follow a deliberate sign convention: `> 0` is success, `0` is no-op, `< 0` is failure. `RainResponse.prepareActionResponse` only maps the three positive success codes to `success = true`; everything else — including `0` and all negative codes — maps to `success = false`.

---

## Dependencies

| Dependency | Version | Scope |
|---|---|---|
| `com.google.code.gson:gson` | `2.11.0` | Compile |
| `org.projectlombok:lombok` | `1.18.36` | Optional (annotation processor) |
| `org.jetbrains:annotations` | `26.0.1` | Compile |
| `com.bloomscorp:bmx-hastar` | `0.0.3` | Compile |

---

## License

BMX RainTree is released under the [MIT License](https://www.mit.edu/~amini/LICENSE.md).

Copyright © 2023 [Bloomscorp](https://github.com/bloomscorp)
