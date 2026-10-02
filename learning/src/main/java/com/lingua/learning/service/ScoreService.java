package com.lingua.learning.service;

import com.lingua.learning.service.dto.ScoreDTO;
import java.util.List;

/**
 * Service Interface for reading the points of learners.
 */
public interface ScoreService {
    /**
     * @return the score of the learner, zero if they never answered.
     */
    ScoreDTO getScore(String userLogin);

    /**
     * @return the best scores, highest first.
     */
    List<ScoreDTO> getLeaderboard(int size);
}
