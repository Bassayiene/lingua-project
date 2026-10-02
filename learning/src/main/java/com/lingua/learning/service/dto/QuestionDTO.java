package com.lingua.learning.service.dto;

import com.lingua.learning.domain.enumeration.Difficulty;
import java.util.List;

/**
 * A question as shown to a learner.
 */
public record QuestionDTO(Long id, String text, Difficulty difficulty, Long categoryId, List<ChoiceDTO> choices) {}
