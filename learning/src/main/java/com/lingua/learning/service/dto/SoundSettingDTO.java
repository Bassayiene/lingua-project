package com.lingua.learning.service.dto;

import com.lingua.learning.domain.enumeration.SoundEvent;
import java.time.Instant;

public record SoundSettingDTO(SoundEvent event, String audioUrl, Instant updatedAt) {}
