package com.lingua.learning.service.dto;

/**
 * Result of an answer.
 *
 * @param correctChoiceId the right choice, so the client can show the correction.
 * @param pointsDelta     points won (positive), lost (negative) or zero.
 * @param totalPoints     total of the learner after this answer.
 * @param soundUrl        sound to play for this outcome, null if the admin configured none.
 */
public record AnswerResultDTO(AnswerOutcome outcome, Long correctChoiceId, int pointsDelta, int totalPoints, String soundUrl) {}
