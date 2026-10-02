package com.lingua.gateway.service.dto;

import java.time.Instant;
import java.util.List;

/**
 * A user account as exposed by the API: never contains the password hash.
 */
public record UserDTO(
    Long id,
    String login,
    String email,
    String firstName,
    String lastName,
    boolean activated,
    Instant createdAt,
    List<String> authorities
) {}
