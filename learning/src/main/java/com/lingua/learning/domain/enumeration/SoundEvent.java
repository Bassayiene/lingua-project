package com.lingua.learning.domain.enumeration;

/**
 * Moments for which the admin can upload a sound.
 */
public enum SoundEvent {
    /** Played after a correct answer. */
    SUCCESS,
    /** Played after a wrong answer or when the timer ran out. */
    FAILURE,
}
