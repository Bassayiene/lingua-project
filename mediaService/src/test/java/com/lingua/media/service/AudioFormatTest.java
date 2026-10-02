package com.lingua.media.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AudioFormatTest {

    @Test
    void wavIsRecognised() {
        assertThat(AudioFormat.detect(ascii("RIFF\0\0\0\0WAVEfmt "))).contains(AudioFormat.WAV);
    }

    @Test
    void oggIsRecognised() {
        assertThat(AudioFormat.detect(ascii("OggS\0\2\0\0"))).contains(AudioFormat.OGG);
    }

    @Test
    void mp3IsRecognisedWithOrWithoutAnId3Tag() {
        assertThat(AudioFormat.detect(ascii("ID3\3\0\0\0\0\0\0"))).contains(AudioFormat.MP3);
        assertThat(AudioFormat.detect(new byte[] { (byte) 0xFF, (byte) 0xFB, (byte) 0x90, 0x00 })).contains(AudioFormat.MP3);
    }

    @Test
    void otherFilesAreNotAudio() {
        // A RIFF container that is not WAVE (AVI video, WebP picture)
        assertThat(AudioFormat.detect(ascii("RIFF\0\0\0\0AVI LIST"))).isEmpty();
        assertThat(AudioFormat.detect(ascii("<html><script>alert(1)</script></html>"))).isEmpty();
        assertThat(AudioFormat.detect(ascii("MZ executable"))).isEmpty();
        assertThat(AudioFormat.detect(new byte[0])).isEmpty();
        assertThat(AudioFormat.detect(new byte[] { (byte) 0xFF })).isEmpty();
    }

    private static byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.ISO_8859_1);
    }
}
