package com.lingua.gateway.web.rest.vm;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/register}.
 */
public record RegisterVM(
    @NotBlank
    @Pattern(regexp = LOGIN_REGEX, message = "3 à 50 caractères : lettres, chiffres, point, tiret ou underscore")
    String login,

    @NotBlank @Email @Size(max = 254) String email,

    @NotBlank @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH) String password,

    @Size(max = 50) String firstName,

    @Size(max = 50) String lastName
) {
    /** No '@' allowed, so that a login can never be mistaken for an email at sign-in. */
    public static final String LOGIN_REGEX = "^[a-zA-Z0-9][a-zA-Z0-9._-]{2,49}$";

    public static final int PASSWORD_MIN_LENGTH = 8;

    public static final int PASSWORD_MAX_LENGTH = 100;
}
