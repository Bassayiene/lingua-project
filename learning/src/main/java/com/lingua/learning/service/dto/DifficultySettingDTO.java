package com.lingua.learning.service.dto;

import com.lingua.learning.domain.enumeration.Difficulty;

public record DifficultySettingDTO(Difficulty difficulty, int points, int penaltyPoints, int timeLimitSeconds) {}
