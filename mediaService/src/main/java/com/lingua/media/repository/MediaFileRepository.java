package com.lingua.media.repository;

import com.lingua.media.domain.MediaFile;
import com.lingua.media.domain.enumeration.MediaKind;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {
    Page<MediaFile> findByKind(MediaKind kind, Pageable pageable);
}
