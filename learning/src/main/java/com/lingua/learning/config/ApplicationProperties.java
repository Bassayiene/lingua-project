package com.lingua.learning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Properties specific to the learning service, under the {@code application} prefix.
 */
@ConfigurationProperties(prefix = "application")
public record ApplicationProperties(Security security, @DefaultValue Attempt attempt) {
    public record Security(Jwt jwt) {}

    public record Jwt(String base64Secret) {}

    /**
     * @param graceSeconds extra time accepted after the end of the timer, to absorb network delay.
     */
    public record Attempt(@DefaultValue("2") long graceSeconds) {}
}
