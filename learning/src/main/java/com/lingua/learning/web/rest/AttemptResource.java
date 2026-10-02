package com.lingua.learning.web.rest;

import com.lingua.learning.service.AttemptService;
import com.lingua.learning.service.dto.AnswerRequest;
import com.lingua.learning.service.dto.AnswerResultDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller receiving the answers of learners.
 */
@RestController
@RequestMapping("/api/attempts")
public class AttemptResource {

    private final AttemptService attemptService;

    public AttemptResource(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    /**
     * {@code POST /attempts/:attemptId/answer} : answer a started question.
     *
     * @return status {@code 200 (OK)} with the outcome: CORRECT, WRONG, or TIME_EXPIRED when the
     *         answer arrived after the end of the timer. Status {@code 400 (Bad Request)} if the choice
     *         is not one of the question, {@code 404 (Not Found)} if the attempt is not one of the
     *         current user, {@code 409 (Conflict)} if it was already answered.
     */
    @PostMapping("/{attemptId}/answer")
    public AnswerResultDTO answer(@PathVariable Long attemptId, @Valid @RequestBody AnswerRequest request) {
        return attemptService.answer(CurrentUser.login(), attemptId, request.choiceId());
    }
}
