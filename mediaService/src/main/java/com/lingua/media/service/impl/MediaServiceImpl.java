package com.lingua.media.service.impl;

import com.lingua.media.config.ApplicationProperties;
import com.lingua.media.domain.MediaFile;
import com.lingua.media.domain.enumeration.MediaKind;
import com.lingua.media.exception.BadRequestException;
import com.lingua.media.exception.NotFoundException;
import com.lingua.media.exception.PayloadTooLargeException;
import com.lingua.media.repository.MediaFileRepository;
import com.lingua.media.service.AudioFormat;
import com.lingua.media.service.ImageProcessor;
import com.lingua.media.service.MediaService;
import com.lingua.media.service.ObjectStorage;
import com.lingua.media.service.dto.MediaFileDTO;
import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service Implementation for managing uploaded images and sounds.
 */
@Service
public class MediaServiceImpl implements MediaService {

    public static final int MAX_IMAGES_PER_UPLOAD = 10;

    private static final int MAX_FILENAME_LENGTH = 255;

    private static final Logger LOG = LoggerFactory.getLogger(MediaServiceImpl.class);

    private final MediaFileRepository mediaFileRepository;

    private final ObjectStorage storage;

    private final ImageProcessor imageProcessor;

    private final ApplicationProperties.Media properties;

    private final Clock clock;

    public MediaServiceImpl(
        MediaFileRepository mediaFileRepository,
        ObjectStorage storage,
        ImageProcessor imageProcessor,
        ApplicationProperties properties,
        Clock clock
    ) {
        this.mediaFileRepository = mediaFileRepository;
        this.storage = storage;
        this.imageProcessor = imageProcessor;
        this.properties = properties.media();
        this.clock = clock;
    }

    @Override
    public List<MediaFileDTO> uploadImages(List<MultipartFile> files, String uploadedBy) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Aucun fichier reçu");
        }
        if (files.size() > MAX_IMAGES_PER_UPLOAD) {
            throw new BadRequestException("Pas plus de " + MAX_IMAGES_PER_UPLOAD + " images par envoi");
        }
        // Convert everything first: if one file is not a picture, nothing is stored
        List<byte[]> jpegs = new ArrayList<>();
        for (MultipartFile file : files) {
            jpegs.add(imageProcessor.toJpeg(read(file), file.getOriginalFilename()));
        }
        List<MediaFileDTO> stored = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            stored.add(
                store(
                    MediaKind.IMAGE,
                    properties.imageBucket(),
                    ImageProcessor.OUTPUT_EXTENSION,
                    ImageProcessor.OUTPUT_CONTENT_TYPE,
                    jpegs.get(i),
                    files.get(i).getOriginalFilename(),
                    uploadedBy
                )
            );
        }
        return stored;
    }

    @Override
    public MediaFileDTO uploadAudio(MultipartFile file, String uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Aucun fichier reçu");
        }
        if (file.getSize() > properties.audioMaxSize().toBytes()) {
            throw new PayloadTooLargeException("Fichier audio trop volumineux (maximum " + properties.audioMaxSize().toMegabytes() + " Mo)");
        }
        byte[] content = read(file);
        AudioFormat format = AudioFormat.detect(content).orElseThrow(() ->
            new BadRequestException("Format audio non pris en charge (MP3, WAV, OGG) : " + file.getOriginalFilename())
        );
        return store(
            MediaKind.AUDIO,
            properties.audioBucket(),
            format.extension(),
            format.contentType(),
            content,
            file.getOriginalFilename(),
            uploadedBy
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MediaFileDTO> findAll(MediaKind kind, Pageable pageable) {
        Page<MediaFile> page = kind == null ? mediaFileRepository.findAll(pageable) : mediaFileRepository.findByKind(kind, pageable);
        return page.map(MediaServiceImpl::toDto);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        MediaFile mediaFile = mediaFileRepository.findById(id).orElseThrow(() -> new NotFoundException("Fichier introuvable : " + id));
        mediaFileRepository.delete(mediaFile);
        mediaFileRepository.flush();
        // Last step: if the storage fails, the transaction rolls back and the row is kept
        storage.remove(mediaFile.getBucket(), mediaFile.getObjectKey());
        LOG.debug("Deleted {} {}", mediaFile.getKind(), mediaFile.getUrl());
    }

    private MediaFileDTO store(
        MediaKind kind,
        String bucket,
        String extension,
        String contentType,
        byte[] content,
        String originalFilename,
        String uploadedBy
    ) {
        UUID id = UUID.randomUUID();
        String objectKey = id + "." + extension;
        String url = storage.store(bucket, objectKey, content, contentType);

        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(id);
        mediaFile.setKind(kind);
        mediaFile.setBucket(bucket);
        mediaFile.setObjectKey(objectKey);
        mediaFile.setFilename(truncate(originalFilename));
        mediaFile.setContentType(contentType);
        mediaFile.setSize(content.length);
        mediaFile.setUrl(url);
        mediaFile.setUploadedBy(uploadedBy);
        mediaFile.setCreatedAt(clock.instant());
        try {
            return toDto(mediaFileRepository.save(mediaFile));
        } catch (RuntimeException e) {
            // Do not leave an object nobody knows about
            try {
                storage.remove(bucket, objectKey);
            } catch (RuntimeException cleanupFailure) {
                LOG.warn("Could not remove orphan object {}/{}", bucket, objectKey, cleanupFailure);
            }
            throw e;
        }
    }

    private static byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Fichier illisible : " + file.getOriginalFilename());
        }
    }

    private static String truncate(String filename) {
        if (filename == null || filename.length() <= MAX_FILENAME_LENGTH) {
            return filename;
        }
        return filename.substring(0, MAX_FILENAME_LENGTH);
    }

    private static MediaFileDTO toDto(MediaFile mediaFile) {
        return new MediaFileDTO(
            mediaFile.getId(),
            mediaFile.getKind(),
            mediaFile.getUrl(),
            mediaFile.getFilename(),
            mediaFile.getContentType(),
            mediaFile.getSize(),
            mediaFile.getUploadedBy(),
            mediaFile.getCreatedAt()
        );
    }
}
