package com.lingua.learning.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * New rules for a difficulty level, sent by the admin.
 *
 * @param points           points won by the first correct answer to a question of this level.
 * @param penaltyPoints    points lost by a wrong answer or an expired timer; 0 disables the penalty.
 * @param timeLimitSeconds time allowed to answer.
 */
public record DifficultySettingUpdate(
    @NotNull @Min(0) @Max(MAX_POINTS) Integer points,
    @NotNull @Min(0) @Max(MAX_POINTS) Integer penaltyPoints,
    @NotNull @Min(MIN_TIME_LIMIT_SECONDS) @Max(MAX_TIME_LIMIT_SECONDS) Integer timeLimitSeconds
) {
    public static final int MAX_POINTS = 10_000;

    public static final int MIN_TIME_LIMIT_SECONDS = 5;

    public static final int MAX_TIME_LIMIT_SECONDS = 600;
}
