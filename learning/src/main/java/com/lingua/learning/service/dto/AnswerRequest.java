package com.lingua.learning.service.dto;

import jakarta.validation.constraints.NotNull;

public record AnswerRequest(@NotNull Long choiceId) {}
