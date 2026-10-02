package com.lingua.gateway.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Properties specific to the gateway, under the {@code application} prefix.
 */
@ConfigurationProperties(prefix = "application")
public record ApplicationProperties(Security security, @DefaultValue Cors cors, BootstrapAdmin bootstrapAdmin) {
    public record Security(Jwt jwt) {}

    public record Jwt(
        String base64Secret,
        @DefaultValue("86400") long tokenValidityInSeconds,
        @DefaultValue("2592000") long tokenValidityInSecondsForRememberMe
    ) {}

    public record Cors(@DefaultValue({}) List<String> allowedOrigins) {}

    /** Admin account created at startup when it does not exist yet. Skipped when login or password is blank. */
    public record BootstrapAdmin(String login, String password) {}
}
