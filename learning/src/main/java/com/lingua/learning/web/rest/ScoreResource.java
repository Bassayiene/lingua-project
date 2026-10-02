package com.lingua.learning.web.rest;

import com.lingua.learning.service.ScoreService;
import com.lingua.learning.service.dto.ScoreDTO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the points of learners.
 */
@RestController
@RequestMapping("/api/scores")
public class ScoreResource {

    private final ScoreService scoreService;

    public ScoreResource(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    /**
     * {@code GET /scores/me} : get the score of the current user.
     */
    @GetMapping("/me")
    public ScoreDTO getMyScore() {
        return scoreService.getScore(CurrentUser.login());
    }

    /**
     * {@code GET /scores/leaderboard} : get the best scores, highest first.
     *
     * @param size number of scores, 100 at most.
     */
    @GetMapping("/leaderboard")
    public List<ScoreDTO> getLeaderboard(@RequestParam(defaultValue = "10") int size) {
        return scoreService.getLeaderboard(size);
    }
}
