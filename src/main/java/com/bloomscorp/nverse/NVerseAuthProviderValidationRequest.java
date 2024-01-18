package com.bloomscorp.nverse;

import com.bloomscorp.nverse.pojo.NVERSE_AUTH_PROVIDER;

public record NVerseAuthProviderValidationRequest(String username, NVERSE_AUTH_PROVIDER provider) {
}
