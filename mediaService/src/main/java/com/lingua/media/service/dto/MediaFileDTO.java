package com.lingua.media.service.dto;

import com.lingua.media.domain.enumeration.MediaKind;
import java.time.Instant;
import java.util.UUID;

/**
 * @param url public URL of the file: this is the value to put in a choice or in a sound setting.
 */
public record MediaFileDTO(
    UUID id,
    MediaKind kind,
    String url,
    String filename,
    String contentType,
    long size,
    String uploadedBy,
    Instant createdAt
) {}
