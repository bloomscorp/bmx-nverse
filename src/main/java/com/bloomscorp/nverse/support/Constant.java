package com.bloomscorp.nverse.support;

/**
 * Library-wide string constants used across the NVerse security and filter layers.
 *
 * <p>This class is non-instantiable. All values are static and final, serving as a
 * single source of truth for header names, HTTP method names, encoding labels, and
 * other primitive sentinels referenced throughout the library.</p>
 */
public final class Constant {

    private Constant() {}

    /** Spring bean scope identifier for singleton-scoped beans. */
    public static final String SCOPE_SINGLETON = "singleton";

    /** The empty string, used as a safe fallback throughout the library. */
    public static final String BLANK_STRING_VALUE = "";

    /** Charset name for UTF-8 encoding, used when reading and writing HTTP request/response bodies. */
    public static final String ENCODING_UTF_8 = "UTF-8";

    /** Null character sentinel used to strip null bytes during input canonicalization. */
    public static final String END_OF_STRING_VALUE = "\0";

    /** HTTP header name for the Authorization header carrying the Bearer JWT token. */
    public static final String REQUEST_HEADER_AUTHORIZATION = "Authorization";

    /** Expected prefix of a valid Bearer token value in the Authorization header. */
    public static final String REQUEST_HEADER_VALUE_BEARER = "Bearer ";

    /** HTTP GET method string for CORS allowed-methods configuration. */
    public static final String METHOD_GET = "GET";

    /** HTTP PUT method string for CORS allowed-methods configuration. */
    public static final String METHOD_PUT = "PUT";

    /** HTTP POST method string for CORS allowed-methods configuration. */
    public static final String METHOD_POST = "POST";

    /** HTTP DELETE method string for CORS allowed-methods configuration. */
    public static final String METHOD_DELETE = "DELETE";

    /** HTTP OPTIONS method string for CORS allowed-methods configuration (preflight). */
    public static final String METHOD_OPTIONS = "OPTIONS";

    /** HTTP HEAD method string for CORS allowed-methods configuration. */
    public static final String METHOD_HEAD = "HEAD";

    /** CORS allowed header: {@code X-Requested-With}, commonly sent by AJAX clients. */
    public static final String HEADER_X_REQUESTED_WITH = "X-Requested-With";

    /** CORS allowed header: {@code Alternates}, used for content negotiation. */
    public static final String HEADER_ALTERNATES = "Alternates";

    /** CORS allowed header: {@code Content-Description}, used for attachment metadata. */
    public static final String HEADER_DESCRIPTION = "Content-Description";

    /** MIME type for JSON responses written directly to the {@code HttpServletResponse}. */
    public static final String RESPONSE_TYPE_APPLICATION_JSON = "application/json";
}
