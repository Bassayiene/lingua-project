package com.lingua.gateway.web.rest.vm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/authenticate}. {@code username} is the login or the email.
 */
public record LoginVM(@NotBlank @Size(max = 254) String username, @NotBlank @Size(max = 100) String password, boolean rememberMe) {}
