package com.lingua.gateway.security;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

/**
 * JWT conventions shared with the microservices: they must use the same algorithm and claim names.
 */
public final class SecurityUtils {

    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

    public static final String AUTHORITIES_CLAIM = "auth";

    public static final String USER_ID_CLAIM = "userId";

    private SecurityUtils() {}
}
