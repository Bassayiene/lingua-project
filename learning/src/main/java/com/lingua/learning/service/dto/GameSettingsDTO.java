package com.lingua.learning.service.dto;

import java.util.List;

/**
 * Everything a client needs before starting a game: rules of each level and sounds to preload.
 * A sound URL is null when the admin configured none.
 */
public record GameSettingsDTO(List<DifficultySettingDTO> difficulties, String successSoundUrl, String failureSoundUrl) {}
