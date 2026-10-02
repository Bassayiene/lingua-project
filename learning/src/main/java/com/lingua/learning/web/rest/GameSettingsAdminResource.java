package com.lingua.learning.web.rest;

import com.lingua.learning.domain.enumeration.Difficulty;
import com.lingua.learning.domain.enumeration.SoundEvent;
import com.lingua.learning.service.GameSettingsService;
import com.lingua.learning.service.dto.DifficultySettingDTO;
import com.lingua.learning.service.dto.DifficultySettingUpdate;
import com.lingua.learning.service.dto.SoundSettingDTO;
import com.lingua.learning.service.dto.SoundSettingUpdate;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the rules the admin can adjust. Reserved to administrators.
 */
@RestController
@RequestMapping("/api/admin/settings")
public class GameSettingsAdminResource {

    private final GameSettingsService gameSettingsService;

    public GameSettingsAdminResource(GameSettingsService gameSettingsService) {
        this.gameSettingsService = gameSettingsService;
    }

    /**
     * {@code GET /admin/settings/difficulties} : get the rules of every difficulty level.
     */
    @GetMapping("/difficulties")
    public List<DifficultySettingDTO> getDifficulties() {
        return gameSettingsService.getDifficulties();
    }

    /**
     * {@code PUT /admin/settings/difficulties/:difficulty} : set the points, the penalty and the
     * time limit of a level. Attempts already started keep their timer.
     */
    @PutMapping("/difficulties/{difficulty}")
    public DifficultySettingDTO updateDifficulty(@PathVariable Difficulty difficulty, @Valid @RequestBody DifficultySettingUpdate update) {
        return gameSettingsService.updateDifficulty(difficulty, update);
    }

    /**
     * {@code GET /admin/settings/sounds} : get the configured sounds.
     */
    @GetMapping("/sounds")
    public List<SoundSettingDTO> getSounds() {
        return gameSettingsService.getSounds();
    }

    /**
     * {@code PUT /admin/settings/sounds/:event} : set the sound of an event (SUCCESS or FAILURE)
     * to an audio file previously uploaded to the media service.
     */
    @PutMapping("/sounds/{event}")
    public SoundSettingDTO setSound(@PathVariable SoundEvent event, @Valid @RequestBody SoundSettingUpdate update) {
        return gameSettingsService.setSound(event, update.audioUrl());
    }

    /**
     * {@code DELETE /admin/settings/sounds/:event} : remove the sound of an event.
     */
    @DeleteMapping("/sounds/{event}")
    public ResponseEntity<Void> removeSound(@PathVariable SoundEvent event) {
        gameSettingsService.removeSound(event);
        return ResponseEntity.noContent().build();
    }
}
