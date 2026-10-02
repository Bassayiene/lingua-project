package com.lingua.learning.service.impl;

import com.lingua.learning.domain.DifficultySetting;
import com.lingua.learning.domain.SoundSetting;
import com.lingua.learning.domain.enumeration.Difficulty;
import com.lingua.learning.domain.enumeration.SoundEvent;
import com.lingua.learning.repository.DifficultySettingRepository;
import com.lingua.learning.repository.SoundSettingRepository;
import com.lingua.learning.service.GameSettingsService;
import com.lingua.learning.service.dto.DifficultySettingDTO;
import com.lingua.learning.service.dto.DifficultySettingUpdate;
import com.lingua.learning.service.dto.GameSettingsDTO;
import com.lingua.learning.service.dto.SoundSettingDTO;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for the rules the admin can adjust.
 */
@Service
@Transactional
public class GameSettingsServiceImpl implements GameSettingsService {

    private static final Logger LOG = LoggerFactory.getLogger(GameSettingsServiceImpl.class);

    private final DifficultySettingRepository difficultySettingRepository;

    private final SoundSettingRepository soundSettingRepository;

    private final Clock clock;

    public GameSettingsServiceImpl(
        DifficultySettingRepository difficultySettingRepository,
        SoundSettingRepository soundSettingRepository,
        Clock clock
    ) {
        this.difficultySettingRepository = difficultySettingRepository;
        this.soundSettingRepository = soundSettingRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public GameSettingsDTO getGameSettings() {
        return new GameSettingsDTO(
            getDifficulties(),
            getSoundUrl(SoundEvent.SUCCESS).orElse(null),
            getSoundUrl(SoundEvent.FAILURE).orElse(null)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DifficultySettingDTO> getDifficulties() {
        return difficultySettingRepository
            .findAll()
            .stream()
            .sorted(Comparator.comparing(DifficultySetting::getDifficulty))
            .map(GameSettingsServiceImpl::toDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DifficultySettingDTO getDifficulty(Difficulty difficulty) {
        return toDto(find(difficulty));
    }

    @Override
    public DifficultySettingDTO updateDifficulty(Difficulty difficulty, DifficultySettingUpdate update) {
        DifficultySetting setting = find(difficulty);
        setting.setPoints(update.points());
        setting.setPenaltyPoints(update.penaltyPoints());
        setting.setTimeLimitSeconds(update.timeLimitSeconds());
        LOG.info(
            "Difficulty {} set to +{} / -{} points, {} s",
            difficulty,
            update.points(),
            update.penaltyPoints(),
            update.timeLimitSeconds()
        );
        return toDto(difficultySettingRepository.save(setting));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoundSettingDTO> getSounds() {
        return soundSettingRepository
            .findAll()
            .stream()
            .sorted(Comparator.comparing(SoundSetting::getEvent))
            .map(GameSettingsServiceImpl::toDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getSoundUrl(SoundEvent event) {
        return soundSettingRepository.findByEvent(event).map(SoundSetting::getAudioUrl);
    }

    @Override
    public SoundSettingDTO setSound(SoundEvent event, String audioUrl) {
        SoundSetting setting = soundSettingRepository
            .findByEvent(event)
            .orElseGet(() -> {
                SoundSetting created = new SoundSetting();
                created.setEvent(event);
                return created;
            });
        setting.setAudioUrl(audioUrl);
        setting.setUpdatedAt(clock.instant());
        return toDto(soundSettingRepository.save(setting));
    }

    @Override
    public void removeSound(SoundEvent event) {
        soundSettingRepository.findByEvent(event).ifPresent(soundSettingRepository::delete);
    }

    private DifficultySetting find(Difficulty difficulty) {
        // One row per level is created by Liquibase in every environment
        return difficultySettingRepository
            .findByDifficulty(difficulty)
            .orElseThrow(() -> new IllegalStateException("No setting for difficulty " + difficulty));
    }

    private static DifficultySettingDTO toDto(DifficultySetting setting) {
        return new DifficultySettingDTO(
            setting.getDifficulty(),
            setting.getPoints(),
            setting.getPenaltyPoints(),
            setting.getTimeLimitSeconds()
        );
    }

    private static SoundSettingDTO toDto(SoundSetting setting) {
        return new SoundSettingDTO(setting.getEvent(), setting.getAudioUrl(), setting.getUpdatedAt());
    }
}
