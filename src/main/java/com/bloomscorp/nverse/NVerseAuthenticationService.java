package com.bloomscorp.nverse;

import com.bloomscorp.nverse.pojo.NVERSE_AUTH_PROVIDER;
import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import com.bloomscorp.pastebox.Pastebox;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class NVerseAuthenticationService {

    public void authenticate(
        String username,
        String password,
        @NotNull AuthenticationManager authenticationManager
    ) throws DisabledException, BadCredentialsException {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                username,
                password
            )
        );
    }

    public <T extends NVerseTenant<E, R>,
        E extends Enum<E>,
        R extends NVerseRole<E>>
    boolean validateAuthProvider(
        @NotNull T tenant,
        NVERSE_AUTH_PROVIDER provider
    ) {
        return (provider != null &&
            Pastebox.isInEnum(provider, NVERSE_AUTH_PROVIDER.class) &&
            tenant.getProvider() == provider
        );
    }
}
