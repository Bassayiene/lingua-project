package com.lingua.learning.service.dto;

import com.lingua.learning.domain.enumeration.Difficulty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * A question as managed by the admin: the question and its choices are saved together.
 * Choices are shown to learners in the order of the list.
 */
public record QuestionAdminDTO(
    Long id,
    @NotBlank @Size(max = 1000) String text,
    @NotNull Difficulty difficulty,
    @NotNull Long categoryId,
    Instant createdAt,
    @NotNull @Valid List<@NotNull ChoiceAdminDTO> choices
) {}
