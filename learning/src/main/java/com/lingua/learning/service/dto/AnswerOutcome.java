package com.lingua.learning.service.dto;

public enum AnswerOutcome {
    CORRECT,
    WRONG,
    /** The answer arrived after the end of the timer and was not taken into account. */
    TIME_EXPIRED,
}
