# bmx-pastebox-java

A lightweight, zero-dependency Java utility library providing a collection of static helper
methods for common development tasks: string validation, enum membership checks, CSV/array
manipulation, reflection utilities, exception formatting, and cryptographically secure random
string generation.

- **Group ID:** `com.bloomscorp`
- **Artifact ID:** `bmx-pastebox-java`
- **Current version:** `0.0.3`
- **Java:** 17+
- **License:** MIT

---

## Table of Contents

1. [Installation](#installation)
2. [Quick Start](#quick-start)
3. [API Reference](#api-reference)
    - [String Validation](#string-validation)
    - [Enum Utilities](#enum-utilities)
    - [Array & CSV Utilities](#array--csv-utilities)
    - [String Manipulation](#string-manipulation)
    - [Type Conversion](#type-conversion)
    - [Reflection Utilities](#reflection-utilities)
    - [Exception Utilities](#exception-utilities)
    - [Random String Generation](#random-string-generation)
4. [Constants](#constants)
5. [Design Notes](#design-notes)

---

## Installation

### Maven

```xml
<dependency>
    <groupId>com.bloomscorp</groupId>
    <artifactId>bmx-pastebox-java</artifactId>
    <version>0.0.3</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
implementation("com.bloomscorp:bmx-pastebox-java:0.0.3")
```

### Gradle (Groovy DSL)

```groovy
implementation 'com.bloomscorp:bmx-pastebox-java:0.0.3'
```

---

## Quick Start

All methods live on the `Pastebox` class as static calls. No instantiation or configuration
is required.

```java
import com.bloomscorp.pastebox.Pastebox;

// String validation
boolean empty = Pastebox.isEmptyString(null);               // true
boolean fits  = Pastebox.minLength("hello", 3);             // true

// Enum check
boolean valid = Pastebox.isInEnum("ACTIVE", Status.class);  // true if ACTIVE exists in Status

// CSV round-trip
String[]  tokens = Pastebox.splitCSVtoStringArray("a,b,c"); // ["a", "b", "c"]
String    csv    = Pastebox.concatStringArrayToCSV(tokens);  // "a,b,c"

// Secure random token
String token = Pastebox.randomAlphaNumericString(32, false); // e.g. "aB3xQ7..."
```

---

## API Reference

### String Validation

#### `isEmptyString(String value)`

Returns `true` if the value is `null`, zero-length after trimming, or whitespace-only.

```java
Pastebox.isEmptyString(null);    // true
Pastebox.isEmptyString("   ");   // true
Pastebox.isEmptyString("hello"); // false
```

---

#### `minLength(String value, int length)`

Returns `true` if `value.length() >= length`. The value must not be `null`.

```java
Pastebox.minLength("hello", 3); // true
Pastebox.minLength("hi", 5);    // false
```

---

#### `maxLength(String value, int length)`

Returns `true` if `value.length() <= length`. The value must not be `null`.

```java
Pastebox.maxLength("hello", 10); // true
Pastebox.maxLength("hello", 3);  // false
```

---

#### `pattern(String value, String pattern)`

Returns `true` if `value` fully matches the given Java regular expression. Returns `false`
if the value is `null`.

```java
Pastebox.pattern("test@example.com", "^[\\w.+-]+@[\\w-]+\\.[\\w.]+$"); // true
Pastebox.pattern("not-an-email", "^[\\w.+-]+@[\\w-]+\\.[\\w.]+$");     // false
Pastebox.pattern(null, ".*");                                            // false
```

---

#### `equalStrings(String s1, String s2)`

Returns `true` if `s1.equals(s2)`. `s1` must not be `null`; `s2` may be `null` (returns `false`).

```java
Pastebox.equalStrings("hello", "hello"); // true
Pastebox.equalStrings("hello", "world"); // false
Pastebox.equalStrings("hello", null);    // false
```

---

#### `containsOnlyAlphanumeric(String value)`

Returns `true` if the value is non-null, non-blank, and contains only `A-Z`, `a-z`, `0-9`.

```java
Pastebox.containsOnlyAlphanumeric("Hello123"); // true
Pastebox.containsOnlyAlphanumeric("hello!");   // false
Pastebox.containsOnlyAlphanumeric(null);       // false
```

---

### Enum Utilities

All enum methods accept a `Class<E>` token. The enum class must not be `null`.

#### `isInEnum(String value, Class<E> enumClass)`

Checks whether a string equals any enum constant's `toString()` (case-sensitive, exact match).

```java
enum Color { RED, GREEN, BLUE }

Pastebox.isInEnum("RED", Color.class);    // true
Pastebox.isInEnum("red", Color.class);    // false — case-sensitive
Pastebox.isInEnum("YELLOW", Color.class); // false
```

---

#### `isInEnum(int value, Class<E> enumClass)`

Checks whether the decimal string form of `value` matches any enum constant's `toString()`.
Useful for enums that override `toString()` to return a numeric code.

```java
enum HttpStatus { /* toString() returns "200", "404", etc. */ }

Pastebox.isInEnum(200, HttpStatus.class); // true if "200" matches any constant
```

---

#### `isInEnum(E enumValue, Class<E> enumClass)`

Checks whether an enum reference is a member of the enum class using reference equality (`==`).

```java
Color c = Color.RED;
Pastebox.isInEnum(c, Color.class); // true
```

---

#### `includesInEnum(String value, Class<E> enumClass)`

Case-insensitive substring check — returns `true` if any enum constant's `toString()` contains `value`.

```java
enum Role { SUPER_ADMIN, CONTENT_ADMIN, VIEWER }

Pastebox.includesInEnum("admin", Role.class); // true — matches SUPER_ADMIN, CONTENT_ADMIN
Pastebox.includesInEnum("owner", Role.class); // false
```

---

### Array & CSV Utilities

#### `splitCSVtoStringArray(String csv)`

Splits a comma-separated string into a `String[]`. Returns an empty array for `null` or blank input.

```java
String[] parts = Pastebox.splitCSVtoStringArray("one,two,three");
// ["one", "two", "three"]

String[] empty = Pastebox.splitCSVtoStringArray(null);
// []
```

---

#### `splitCSVtoLongArray(String csv)`

Splits a CSV string and parses each token as a `long`. Returns an empty array on blank input
or if any token is not a valid long (no exception is thrown).

```java
long[] ids = Pastebox.splitCSVtoLongArray("1,2,3");
// [1L, 2L, 3L]

long[] safe = Pastebox.splitCSVtoLongArray("1,abc,3");
// [] — graceful fallback on parse failure
```

---

#### `concatStringArrayToCSV(String[] array)`

Joins a `String[]` into a CSV string using `","` as delimiter. Returns an empty string for
`null` or empty arrays.

```java
String csv = Pastebox.concatStringArrayToCSV(new String[]{"a", "b", "c"});
// "a,b,c"

String empty = Pastebox.concatStringArrayToCSV(null);
// ""
```

---

#### `stringArrayContainsValue(String[] array, String value)`

Returns `true` if the array contains an element equal to `value`.

```java
String[] roles = {"ADMIN", "USER"};
Pastebox.stringArrayContainsValue(roles, "ADMIN"); // true
Pastebox.stringArrayContainsValue(roles, "GUEST"); // false
```

---

#### `intArrayContainsValue(int[] array, int value)`

Returns `true` if the primitive `int` array contains `value`.

```java
int[] codes = {200, 201, 204};
Pastebox.intArrayContainsValue(codes, 204); // true
Pastebox.intArrayContainsValue(codes, 404); // false
```

---

#### `doubleArrayContainsValue(double[] array, double value)`

Returns `true` if the primitive `double` array contains `value` (exact IEEE 754 equality).

```java
double[] scores = {0.5, 1.0, 1.5};
Pastebox.doubleArrayContainsValue(scores, 1.0); // true
```

---

#### `longArrayContainsValue(long[] array, long value)`

Returns `true` if the primitive `long` array contains `value`.

```java
long[] ids = {100L, 200L, 300L};
Pastebox.longArrayContainsValue(ids, 200L); // true
```

---

#### `removeElementFromStringArray(String[] array, String value)`

Returns a new array with all occurrences of `value` removed. Order is preserved.

```java
String[] result = Pastebox.removeElementFromStringArray(
    new String[]{"a", "b", "a", "c"}, "a"
);
// ["b", "c"]
```

---

#### `isEmptyArray(Object[] array)`

Returns `true` if the array is `null` or has length zero.

```java
Pastebox.isEmptyArray(null);           // true
Pastebox.isEmptyArray(new String[0]);  // true
Pastebox.isEmptyArray(new String[]{"x"}); // false
```

---

### String Manipulation

#### `removeSpaces(String value)`

Removes every space character (including internal spaces) and trims the result.

```java
Pastebox.removeSpaces("  hello world  "); // "helloworld"
```

---

#### `removeExtraSpaces(String value)`

Trims and collapses runs of spaces into a single space.

```java
Pastebox.removeExtraSpaces("  hello   world  "); // "hello world"
```

---

#### `replaceMultipleSpaceWithSingleSpace(String value)`

Collapses consecutive spaces into one without trimming leading/trailing spaces.

```java
Pastebox.replaceMultipleSpaceWithSingleSpace("a  b   c"); // "a b c"
```

---

#### `removeNewLineReturnTabWithSpace(String value)`

Replaces `\t`, `\n`, and `\r` characters (and consecutive runs of them) with a single space.

```java
Pastebox.removeNewLineReturnTabWithSpace("line1\n\nline2\ttab"); // "line1 line2 tab"
```

---

#### `keepAlphanumericAndSpace(String value)`

Strips every character that is not alphanumeric or a space.

```java
Pastebox.keepAlphanumericAndSpace("Hello, World! #2024"); // "Hello World 2024"
```

---

#### `getNullSafeString(String value)`

Returns the value as-is if non-null and non-blank; otherwise returns an empty string `""`.

```java
Pastebox.getNullSafeString(null);    // ""
Pastebox.getNullSafeString("  ");    // ""
Pastebox.getNullSafeString("hello"); // "hello"
```

---

### Type Conversion

#### `getLongFromStringValue(String value)`

Parses a `long` from a decimal string. Throws `NumberFormatException` on invalid input.

```java
long id = Pastebox.getLongFromStringValue("12345"); // 12345L
```

---

#### `replaceStringWithBoolean(String value)`

Returns `true` only if `value` equals `"yes"` (case-insensitive). All other values,
including `"true"`, `"1"`, `null`, or blank, return `false`.

```java
Pastebox.replaceStringWithBoolean("yes");  // true
Pastebox.replaceStringWithBoolean("YES");  // true
Pastebox.replaceStringWithBoolean("true"); // false
Pastebox.replaceStringWithBoolean(null);   // false
```

---

#### `getCurrentTimeInMillis()`

Returns the current epoch time in milliseconds via `System.currentTimeMillis()`.

```java
long now = Pastebox.getCurrentTimeInMillis();
```

---

### Reflection Utilities

These methods simplify working with JavaBean conventions and the Reflection API.

#### `prepareGetterMethodName(String field)`

Constructs the conventional getter name for a field.

```java
Pastebox.prepareGetterMethodName("userName"); // "getUserName"
Pastebox.prepareGetterMethodName("id");       // "getId"
```

---

#### `prepareSetterMethodName(String field)`

Constructs the conventional setter name for a field.

```java
Pastebox.prepareSetterMethodName("userName"); // "setUserName"
```

---

#### `getClassFields(Object object)`

Returns the names of all fields declared directly on the object's class (excludes inherited
fields).

```java
class User { private String name; private int age; }

List<String> fields = Pastebox.getClassFields(new User());
// ["name", "age"]
```

---

#### `getFieldGetterMethod(String field, Object object)`

Looks up the public getter `Method` for a named field on the object's class via reflection.
Throws `NoSuchMethodException` if the getter does not exist or is not public.

```java
Method getter = Pastebox.getFieldGetterMethod("name", user);
String value  = (String) getter.invoke(user);
```

---

#### `getFieldSetterMethod(String field, Class<?> param, Object object)`

Looks up the public setter `Method` for a named field. `param` is the expected parameter
type of the setter.

```java
Method setter = Pastebox.getFieldSetterMethod("name", String.class, user);
setter.invoke(user, "Alice");
```

---

#### `getFieldNameFromGetterMethod(Method method)`

Extracts the field name from a getter `Method` by stripping the `"get"` prefix and
lowercasing the first character. Returns an empty string if the method is not a getter.

```java
Method getter    = user.getClass().getMethod("getUserName");
String fieldName = Pastebox.getFieldNameFromGetterMethod(getter);
// "userName"
```

---

#### `fieldHasType(String fieldName, Class<?> type, Object object)`

Returns `true` if the named declared field's type equals `type`. Throws `NoSuchFieldException`
if the field does not exist.

```java
Pastebox.fieldHasType("name", String.class, user);  // true
Pastebox.fieldHasType("name", Integer.class, user); // false
```

---

#### `methodHasReturnType(Method method, Class<?> returnType)`

Returns `true` if the method's return type equals `returnType`. Returns `false` if
`returnType` is `null`.

```java
Method getter = user.getClass().getMethod("getName");
Pastebox.methodHasReturnType(getter, String.class);  // true
Pastebox.methodHasReturnType(getter, Integer.class); // false
```

---

### Exception Utilities

#### `getStackTraceAsString(Exception exception, String separator)`

Renders the full stack trace — and its cause chain — as a plain string. The cause trace
is appended after `separator` when a cause exists.

```java
try {
    riskyOperation();
} catch (Exception e) {
    String trace = Pastebox.getStackTraceAsString(e, "\n--- Caused by ---\n");
    logger.error(trace);
}
```

---

### Random String Generation

#### `randomAlphaNumericString(int length, boolean uppercaseOnly)`

Generates a cryptographically secure random string using `SecureRandom`.

| `uppercaseOnly` | Character pool | Pool size |
|---|---|---|
| `false` | `A-Z`, `a-z`, `0-9` | 62 |
| `true` | `A-Z`, `0-9` | 36 |

```java
// Mixed-case token (e.g. session ID)
String sessionToken = Pastebox.randomAlphaNumericString(64, false);

// Uppercase-only code (e.g. referral or promo code)
String promoCode = Pastebox.randomAlphaNumericString(8, true);
// e.g. "A3XQ7B2K"
```

> **Security note:** The underlying `SecureRandom` instance is shared and reused across calls.
> Output is suitable for security-sensitive use cases such as tokens, OTPs, and identifiers.

---

## Constants

`com.bloomscorp.pastebox.support.Constant` exposes one shared constant:

| Constant | Value | Purpose |
|---|---|---|
| `BLANK_STRING_VALUE` | `""` | Safe empty-string default used throughout the library |

```java
import com.bloomscorp.pastebox.support.Constant;

String safe = Constant.BLANK_STRING_VALUE; // ""
```

---

## Design Notes

- **Non-instantiable** — `Pastebox` and `Constant` both have private constructors. All
  functionality is accessed through static methods.
- **Null safety** — Methods that cannot tolerate `null` inputs are annotated with
  `@NotNull` (JetBrains Annotations). Methods that accept `null` document the behaviour
  explicitly.
- **No checked exceptions from callers** — CSV and array methods return empty results on
  invalid input rather than throwing. Only reflection methods (`getFieldGetterMethod`,
  `getFieldSetterMethod`, `fieldHasType`) propagate `NoSuchMethodException` /
  `NoSuchFieldException` because the caller must handle missing members explicitly.
- **Cryptographic randomness** — `randomAlphaNumericString` uses `java.security.SecureRandom`,
  not `java.util.Random`, making it safe for token and code generation.
- **Java 17** — The library targets Java 17 and uses records-era stream APIs
  (`Stream.toList()` from Java 16+). It does not use any Spring or Jakarta EE APIs and
  is framework-agnostic.
