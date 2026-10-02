package com.lingua.learning.service;

import com.lingua.learning.domain.enumeration.Difficulty;
import com.lingua.learning.domain.enumeration.SoundEvent;
import com.lingua.learning.service.dto.DifficultySettingDTO;
import com.lingua.learning.service.dto.DifficultySettingUpdate;
import com.lingua.learning.service.dto.GameSettingsDTO;
import com.lingua.learning.service.dto.SoundSettingDTO;
import java.util.List;
import java.util.Optional;

/**
 * Service Interface for the rules the admin can adjust: points, penalty and timer of each
 * difficulty level, and the sounds played after an answer.
 */
public interface GameSettingsService {
    /**
     * Rules of every level and sound URLs, for the client.
     */
    GameSettingsDTO getGameSettings();

    List<DifficultySettingDTO> getDifficulties();

    DifficultySettingDTO getDifficulty(Difficulty difficulty);

    /**
     * Change the rules of a level. Attempts already started keep their timer.
     */
    DifficultySettingDTO updateDifficulty(Difficulty difficulty, DifficultySettingUpdate update);

    List<SoundSettingDTO> getSounds();

    /**
     * @return the URL of the sound for the event, empty if the admin configured none.
     */
    Optional<String> getSoundUrl(SoundEvent event);

    SoundSettingDTO setSound(SoundEvent event, String audioUrl);

    void removeSound(SoundEvent event);
}
