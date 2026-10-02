package com.lingua.learning.service.dto;

import java.time.Instant;

/**
 * A question handed to a learner, with its running timer.
 *
 * @param attemptId        to send back with the answer.
 * @param timeLimitSeconds total time allowed for this attempt.
 * @param remainingSeconds time left on the server clock: use it for the countdown rather than
 *                         {@code expiresAt}, which depends on the clock of the device.
 */
public record StartedQuestionDTO(
    Long attemptId,
    QuestionDTO question,
    long timeLimitSeconds,
    long remainingSeconds,
    Instant startedAt,
    Instant expiresAt
) {}
