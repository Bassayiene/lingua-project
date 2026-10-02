package com.lingua.learning.service;

import com.lingua.learning.domain.enumeration.Difficulty;
import com.lingua.learning.service.dto.AnswerResultDTO;
import com.lingua.learning.service.dto.StartedQuestionDTO;
import java.util.Optional;

/**
 * Service Interface for the timed attempts of learners: handing out a question starts a timer,
 * answering closes the attempt and updates the points.
 */
public interface AttemptService {
    /**
     * Hand the learner a question they have not answered correctly yet, and start its timer.
     * While an attempt is still running, that same attempt is returned: asking again never
     * restarts the timer or swaps the question.
     *
     * @param categoryId optional filter.
     * @param difficulty optional filter.
     * @return the started question, empty when there is nothing left to answer.
     */
    Optional<StartedQuestionDTO> next(String userLogin, Long categoryId, Difficulty difficulty);

    /**
     * Answer a running attempt. After the end of the timer the answer is not taken into account:
     * the attempt expires and the penalty applies.
     */
    AnswerResultDTO answer(String userLogin, Long attemptId, Long choiceId);

    /**
     * Close the attempts the learner left unanswered past their timer, applying the penalty.
     * Called before anything that reads or changes the points of the learner.
     */
    void settleExpired(String userLogin);
}
