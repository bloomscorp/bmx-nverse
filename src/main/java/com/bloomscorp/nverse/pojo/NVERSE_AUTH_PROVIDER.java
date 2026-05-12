package com.bloomscorp.nverse.pojo;

/**
 * Enumerates the authentication providers supported by the NVerse security layer.
 *
 * <p>The ordinal position of each constant is meaningful and must not be changed —
 * persisted integer representations rely on declaration order. Adding new providers
 * must always be done at the end of the list.</p>
 *
 * <ul>
 *   <li>{@link #UNKNOWN} — provider could not be determined or was not specified.</li>
 *   <li>{@link #BASIC} — standard username/password authentication.</li>
 *   <li>{@link #GOOGLE} — OAuth 2.0 authentication via Google.</li>
 *   <li>{@link #FACEBOOK} — OAuth 2.0 authentication via Facebook.</li>
 * </ul>
 */
public enum NVERSE_AUTH_PROVIDER {
    UNKNOWN,
    BASIC,
    GOOGLE,
    FACEBOOK
}
