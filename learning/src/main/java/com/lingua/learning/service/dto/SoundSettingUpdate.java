package com.lingua.learning.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param audioUrl URL returned by the media service after the upload of the audio file.
 */
public record SoundSettingUpdate(
    @NotBlank @Size(max = 1000) @Pattern(regexp = "^https?://\\S+$", message = "doit être une URL http(s)") String audioUrl
) {}
