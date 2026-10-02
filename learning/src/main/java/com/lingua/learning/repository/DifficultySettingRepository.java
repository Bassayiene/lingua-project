package com.lingua.learning.repository;

import com.lingua.learning.domain.DifficultySetting;
import com.lingua.learning.domain.enumeration.Difficulty;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DifficultySettingRepository extends JpaRepository<DifficultySetting, Long> {
    Optional<DifficultySetting> findByDifficulty(Difficulty difficulty);
}
