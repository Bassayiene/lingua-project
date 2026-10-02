package com.lingua.learning.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LanguageDTO(Long id, @NotBlank @Size(max = 10) String code, @NotBlank @Size(max = 100) String name) {}
