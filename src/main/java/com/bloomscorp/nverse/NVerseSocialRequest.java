package com.bloomscorp.nverse;

import com.bloomscorp.nverse.pojo.NVERSE_AUTH_PROVIDER;

public record NVerseSocialRequest(String username, String password, NVERSE_AUTH_PROVIDER provider) {
}
