package com.lingua.learning.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param languageCode read-only, filled in responses.
 */
public record CategoryDTO(
    Long id,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description,
    @NotNull Long languageId,
    String languageCode
) {}
