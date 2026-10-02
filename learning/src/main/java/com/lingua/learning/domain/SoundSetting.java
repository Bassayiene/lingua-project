package com.lingua.learning.domain;

import com.lingua.learning.domain.enumeration.SoundEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Sound played by the client for an event. At most one row per event; no row means no sound.
 */
@Entity
@Table(name = "sound_setting")
public class SoundSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event", nullable = false, unique = true, length = 20)
    private SoundEvent event;

    /** URL of the audio file, returned by the media service. */
    @Column(name = "audio_url", nullable = false, length = 1000)
    private String audioUrl;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SoundEvent getEvent() {
        return event;
    }

    public void setEvent(SoundEvent event) {
        this.event = event;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
