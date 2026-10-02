package com.lingua.media.web.rest;

import com.lingua.media.domain.enumeration.MediaKind;
import com.lingua.media.security.SecurityUtils;
import com.lingua.media.service.MediaService;
import com.lingua.media.service.dto.MediaFileDTO;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for uploading and managing images and sounds. Reserved to administrators.
 * The files themselves are read straight from MinIO, with the URL returned by the uploads.
 */
@RestController
@RequestMapping("/api/media")
public class MediaResource {

    private final MediaService mediaService;

    public MediaResource(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /**
     * {@code POST /media/images/upload} : upload one or more pictures (multipart field {@code files}).
     * They are stored as JPEG, scaled down if larger than 800x600.
     *
     * @return the stored files with their URL, or status {@code 400 (Bad Request)} if a file is not
     *         a readable picture, in which case nothing is stored.
     */
    @PostMapping(value = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<MediaFileDTO> uploadImages(@RequestParam("files") List<MultipartFile> files) {
        return mediaService.uploadImages(files, currentUserLogin());
    }

    /**
     * {@code POST /media/audio/upload} : upload a sound (multipart field {@code file}), MP3, WAV or OGG.
     *
     * @return the stored file with its URL, status {@code 400 (Bad Request)} if it is not an accepted
     *         audio format, or {@code 413 (Payload Too Large)} if it is over the size limit.
     */
    @PostMapping(value = "/audio/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MediaFileDTO uploadAudio(@RequestParam("file") MultipartFile file) {
        return mediaService.uploadAudio(file, currentUserLogin());
    }

    /**
     * {@code GET /media} : get a page of stored files, most recent first.
     * The total number of files is returned in the {@code X-Total-Count} header.
     *
     * @param kind only IMAGE or only AUDIO; both if absent.
     */
    @GetMapping("")
    public ResponseEntity<List<MediaFileDTO>> getMediaFiles(
        @RequestParam(required = false) MediaKind kind,
        @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MediaFileDTO> page = mediaService.findAll(kind, pageable);
        return ResponseEntity.ok().header("X-Total-Count", String.valueOf(page.getTotalElements())).body(page.getContent());
    }

    /**
     * {@code DELETE /media/:id} : delete a file from the storage. Choices or sound settings still
     * pointing to its URL are not updated.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMediaFile(@PathVariable UUID id) {
        mediaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static String currentUserLogin() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
