package com.lingua.learning.config;

import static com.lingua.learning.security.SecurityUtils.AUTHORITIES_CLAIM;
import static com.lingua.learning.security.SecurityUtils.JWT_ALGORITHM;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Validates the JWT issued by the gateway: same secret, same algorithm, same claim names.
 */
@Configuration
public class SecurityJwtConfiguration {

    private final ApplicationProperties properties;

    public SecurityJwtConfiguration(ApplicationProperties properties) {
        this.properties = properties;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(getSecretKey()).macAlgorithm(JWT_ALGORITHM).build();
    }

    /**
     * Reads the roles from the {@code auth} claim, as written, without the default {@code SCOPE_} prefix.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(AUTHORITIES_CLAIM);
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    private SecretKey getSecretKey() {
        // A secret pasted into a .env file often carries a stray line break (CR on Windows) or space
        String secret = properties.security().jwt().base64Secret().replaceAll("[\\s\\r\\n]", "");
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
    }
}
