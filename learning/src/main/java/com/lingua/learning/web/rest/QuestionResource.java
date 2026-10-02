package com.lingua.learning.web.rest;

import com.lingua.learning.domain.enumeration.Difficulty;
import com.lingua.learning.service.AttemptService;
import com.lingua.learning.service.dto.StartedQuestionDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller handing questions to learners.
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionResource {

    private final AttemptService attemptService;

    public QuestionResource(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    /**
     * {@code POST /questions/next} : get a question not answered correctly yet and start its timer.
     * While an attempt is running, the same question and the same timer are returned.
     * It is a POST because it starts an attempt.
     *
     * @param categoryId optional filter.
     * @param difficulty optional filter.
     * @return the question (without the correct answer) with status {@code 200 (OK)},
     *         or status {@code 204 (No Content)} when nothing is left to answer.
     */
    @PostMapping("/next")
    public ResponseEntity<StartedQuestionDTO> nextQuestion(
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Difficulty difficulty
    ) {
        return attemptService
            .next(CurrentUser.login(), categoryId, difficulty)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
