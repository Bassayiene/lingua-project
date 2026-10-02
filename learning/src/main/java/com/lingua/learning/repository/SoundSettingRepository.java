package com.lingua.learning.repository;

import com.lingua.learning.domain.SoundSetting;
import com.lingua.learning.domain.enumeration.SoundEvent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SoundSettingRepository extends JpaRepository<SoundSetting, Long> {
    Optional<SoundSetting> findByEvent(SoundEvent event);
}
