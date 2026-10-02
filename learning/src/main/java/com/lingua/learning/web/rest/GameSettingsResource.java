package com.lingua.learning.web.rest;

import com.lingua.learning.service.GameSettingsService;
import com.lingua.learning.service.dto.GameSettingsDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller giving clients the rules of the game.
 */
@RestController
@RequestMapping("/api/settings")
public class GameSettingsResource {

    private final GameSettingsService gameSettingsService;

    public GameSettingsResource(GameSettingsService gameSettingsService) {
        this.gameSettingsService = gameSettingsService;
    }

    /**
     * {@code GET /settings/game} : points, penalty and time limit of each difficulty level, and the
     * URLs of the success and failure sounds so the client can preload them.
     */
    @GetMapping("/game")
    public GameSettingsDTO getGameSettings() {
        return gameSettingsService.getGameSettings();
    }
}
