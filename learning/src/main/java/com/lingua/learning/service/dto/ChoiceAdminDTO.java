package com.lingua.learning.service.dto;

import com.lingua.learning.domain.enumeration.ChoiceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A choice as managed by the admin, with the correct flag.
 * {@code text} is required for a TEXT choice, {@code imageUrl} for an IMAGE choice.
 */
public record ChoiceAdminDTO(
    Long id,
    @NotNull ChoiceType type,
    @Size(max = 500) String text,
    @Size(max = 1000) String imageUrl,
    boolean correct
) {}
