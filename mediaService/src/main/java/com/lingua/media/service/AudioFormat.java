package com.lingua.media.service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Audio formats accepted for sounds. The format is recognised from the first bytes of the file,
 * not from the name or the content type sent by the client, which anyone can forge.
 */
public enum AudioFormat {
    MP3("audio/mpeg", "mp3"),
    WAV("audio/wav", "wav"),
    OGG("audio/ogg", "ogg");

    private final String contentType;

    private final String extension;

    AudioFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    public static Optional<AudioFormat> detect(byte[] content) {
        if (startsWith(content, 0, "RIFF") && startsWith(content, 8, "WAVE")) {
            return Optional.of(WAV);
        }
        if (startsWith(content, 0, "OggS")) {
            return Optional.of(OGG);
        }
        if (startsWith(content, 0, "ID3") || isMpegAudioFrame(content)) {
            return Optional.of(MP3);
        }
        return Optional.empty();
    }

    /** An MP3 without ID3 tag starts with a frame header: 11 sync bits set, layer III. */
    private static boolean isMpegAudioFrame(byte[] content) {
        return content.length >= 2 && (content[0] & 0xFF) == 0xFF && (content[1] & 0xE6) == 0xE2;
    }

    private static boolean startsWith(byte[] content, int offset, String magic) {
        byte[] expected = magic.getBytes(StandardCharsets.US_ASCII);
        if (content.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (content[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
