package com.lingua.learning.service.dto;

/**
 * @param correctAnswers number of distinct questions answered correctly.
 */
public record ScoreDTO(String userLogin, int totalPoints, int correctAnswers) {}
