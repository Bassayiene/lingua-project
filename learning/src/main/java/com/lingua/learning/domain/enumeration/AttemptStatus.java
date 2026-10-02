package com.lingua.learning.domain.enumeration;

public enum AttemptStatus {
    /** The question was handed to the learner and the timer is running. */
    OPEN,
    CORRECT,
    WRONG,
    /** The timer ran out before an answer was received. */
    EXPIRED,
}
