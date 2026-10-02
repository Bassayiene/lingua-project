package com.lingua.media.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lingua.media.config.ApplicationProperties;
import com.lingua.media.domain.MediaFile;
import com.lingua.media.domain.enumeration.MediaKind;
import com.lingua.media.exception.BadRequestException;
import com.lingua.media.exception.PayloadTooLargeException;
import com.lingua.media.exception.StorageException;
import com.lingua.media.repository.MediaFileRepository;
import com.lingua.media.service.ImageProcessor;
import com.lingua.media.service.ObjectStorage;
import com.lingua.media.service.dto.MediaFileDTO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MediaServiceImplTest {

    private static final String ADMIN = "admin";

    @Mock
    private MediaFileRepository mediaFileRepository;

    @Mock
    private ObjectStorage storage;

    private MediaServiceImpl service;

    @BeforeEach
    void setUp() {
        ApplicationProperties properties = new ApplicationProperties(
            null,
            null,
            new ApplicationProperties.Media("learning-images", "learning-audio", 800, 600, DataSize.ofKilobytes(1))
        );
        when(storage.store(any(), any(), any(), any())).thenAnswer(invocation ->
            "http://minio/" + invocation.getArgument(0) + "/" + invocation.getArgument(1)
        );
        when(mediaFileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service = new MediaServiceImpl(
            mediaFileRepository,
            storage,
            new ImageProcessor(properties),
            properties,
            Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void pictureIsStoredAsJpegInTheImageBucket() throws IOException {
        MediaFileDTO stored = service.uploadImages(List.of(file("cat.png", "image/png", png())), ADMIN).get(0);

        assertThat(stored.kind()).isEqualTo(MediaKind.IMAGE);
        assertThat(stored.contentType()).isEqualTo("image/jpeg");
        assertThat(stored.filename()).isEqualTo("cat.png");
        assertThat(stored.uploadedBy()).isEqualTo(ADMIN);
        // The object is named after the generated id, never after the uploaded file name
        assertThat(stored.url()).isEqualTo("http://minio/learning-images/" + stored.id() + ".jpg");
        verify(storage).store(eq("learning-images"), eq(stored.id() + ".jpg"), any(), eq("image/jpeg"));
    }

    @Test
    void nothingIsStoredWhenOneOfTheFilesIsNotAPicture() throws IOException {
        List<MultipartFile> files = List.of(file("cat.png", "image/png", png()), file("notes.png", "image/png", text("not a picture")));

        assertThatThrownBy(() -> service.uploadImages(files, ADMIN)).isInstanceOf(BadRequestException.class);
        verify(storage, never()).store(any(), any(), any(), any());
        verify(mediaFileRepository, never()).save(any());
    }

    @Test
    void emptyUploadIsRefused() {
        assertThatThrownBy(() -> service.uploadImages(List.of(), ADMIN)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void soundIsStoredAsItIsInTheAudioBucket() {
        byte[] ogg = text("OggS-sound-data");

        // The client says "audio/mpeg" and names it .mp3: the content decides
        MediaFileDTO stored = service.uploadAudio(file("bravo.mp3", "audio/mpeg", ogg), ADMIN);

        assertThat(stored.kind()).isEqualTo(MediaKind.AUDIO);
        assertThat(stored.contentType()).isEqualTo("audio/ogg");
        assertThat(stored.size()).isEqualTo(ogg.length);
        verify(storage).store("learning-audio", stored.id() + ".ogg", ogg, "audio/ogg");
    }

    @Test
    void fileThatIsNotAudioIsRefusedWhateverItsDeclaredType() {
        MultipartFile html = file("sound.mp3", "audio/mpeg", text("<html><script>alert(1)</script></html>"));

        assertThatThrownBy(() -> service.uploadAudio(html, ADMIN)).isInstanceOf(BadRequestException.class);
        verify(storage, never()).store(any(), any(), any(), any());
    }

    @Test
    void soundOverTheSizeLimitIsRefused() {
        byte[] tooBig = new byte[2000];
        System.arraycopy(text("OggS"), 0, tooBig, 0, 4);

        assertThatThrownBy(() -> service.uploadAudio(file("long.ogg", "audio/ogg", tooBig), ADMIN)).isInstanceOf(
            PayloadTooLargeException.class
        );
        verify(storage, never()).store(any(), any(), any(), any());
    }

    @Test
    void objectIsRemovedWhenItsRowCannotBeSaved() {
        when(mediaFileRepository.save(any())).thenThrow(new IllegalStateException("database down"));

        assertThatThrownBy(() -> service.uploadAudio(file("ok.ogg", "audio/ogg", text("OggS-sound")), ADMIN)).isInstanceOf(
            IllegalStateException.class
        );

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(storage).store(eq("learning-audio"), key.capture(), any(), any());
        verify(storage).remove("learning-audio", key.getValue());
    }

    @Test
    void deleteRemovesTheRowAndTheObject() {
        MediaFile mediaFile = mediaFile();
        when(mediaFileRepository.findById(mediaFile.getId())).thenReturn(Optional.of(mediaFile));

        service.delete(mediaFile.getId());

        verify(mediaFileRepository).delete(mediaFile);
        verify(storage).remove("learning-images", mediaFile.getObjectKey());
    }

    @Test
    void deleteFailsWhenTheStorageIsDownSoTheRowIsRolledBack() {
        MediaFile mediaFile = mediaFile();
        when(mediaFileRepository.findById(mediaFile.getId())).thenReturn(Optional.of(mediaFile));
        doThrow(new StorageException("down", null)).when(storage).remove(any(), any());

        assertThatThrownBy(() -> service.delete(mediaFile.getId())).isInstanceOf(StorageException.class);
    }

    private static MediaFile mediaFile() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(UUID.randomUUID());
        mediaFile.setKind(MediaKind.IMAGE);
        mediaFile.setBucket("learning-images");
        mediaFile.setObjectKey(mediaFile.getId() + ".jpg");
        return mediaFile;
    }

    private static MultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    private static byte[] text(String value) {
        return value.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static byte[] png() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(100, 60, BufferedImage.TYPE_INT_RGB), "png", output);
        return output.toByteArray();
    }
}
