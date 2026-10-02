package com.lingua.media.service;

import com.lingua.media.domain.enumeration.MediaKind;
import com.lingua.media.service.dto.MediaFileDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service Interface for managing uploaded images and sounds.
 */
public interface MediaService {
    /**
     * Store pictures as JPEG, scaled down if needed. Nothing is stored if one of them is not a
     * readable picture.
     */
    List<MediaFileDTO> uploadImages(List<MultipartFile> files, String uploadedBy);

    /**
     * Store an MP3, WAV or OGG file as it is.
     */
    MediaFileDTO uploadAudio(MultipartFile file, String uploadedBy);

    /**
     * @param kind optional filter.
     */
    Page<MediaFileDTO> findAll(MediaKind kind, Pageable pageable);

    /**
     * Delete the file from the storage and from the database.
     */
    void delete(UUID id);
}
