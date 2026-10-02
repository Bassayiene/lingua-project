package com.lingua.learning.service.dto;

import com.lingua.learning.domain.enumeration.ChoiceType;

/**
 * A choice as shown to a learner: it must never say whether the choice is the correct one.
 */
public record ChoiceDTO(Long id, ChoiceType type, String text, String imageUrl) {}
