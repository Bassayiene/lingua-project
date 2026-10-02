package com.lingua.learning.service.impl;

import com.lingua.learning.domain.LearnerScore;
import com.lingua.learning.repository.LearnerScoreRepository;
import com.lingua.learning.service.AttemptService;
import com.lingua.learning.service.ScoreService;
import com.lingua.learning.service.dto.ScoreDTO;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for reading the points of learners.
 */
@Service
@Transactional
public class ScoreServiceImpl implements ScoreService {

    public static final int MAX_LEADERBOARD_SIZE = 100;

    private final LearnerScoreRepository scoreRepository;

    private final AttemptService attemptService;

    public ScoreServiceImpl(LearnerScoreRepository scoreRepository, AttemptService attemptService) {
        this.scoreRepository = scoreRepository;
        this.attemptService = attemptService;
    }

    @Override
    public ScoreDTO getScore(String userLogin) {
        // An attempt left unanswered past its timer costs its penalty before the score is shown
        attemptService.settleExpired(userLogin);
        return scoreRepository.findByUserLogin(userLogin).map(ScoreServiceImpl::toDto).orElseGet(() -> new ScoreDTO(userLogin, 0, 0));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreDTO> getLeaderboard(int size) {
        // On equal points, the learner who reached them first is ranked first
        PageRequest page = PageRequest.of(
            0,
            Math.min(Math.max(size, 1), MAX_LEADERBOARD_SIZE),
            Sort.by(Sort.Order.desc("totalPoints"), Sort.Order.asc("updatedAt"))
        );
        return scoreRepository.findAll(page).stream().map(ScoreServiceImpl::toDto).toList();
    }

    private static ScoreDTO toDto(LearnerScore score) {
        return new ScoreDTO(score.getUserLogin(), score.getTotalPoints(), score.getCorrectAnswers());
    }
}
