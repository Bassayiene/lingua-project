package com.lingua.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Properties specific to the media service, under the {@code application} prefix.
 */
@ConfigurationProperties(prefix = "application")
public record ApplicationProperties(Security security, Minio minio, @DefaultValue Media media) {
    public record Security(Jwt jwt) {}

    public record Jwt(String base64Secret) {}

    /**
     * @param endpoint  address of MinIO as seen by this service.
     * @param publicUrl address of MinIO as seen by the browser, used to build the URL of stored files.
     */
    public record Minio(String endpoint, String publicUrl, String accessKey, String secretKey) {}

    /**
     * @param imageMaxWidth  larger images are scaled down to fit in this box.
     * @param imageMaxHeight larger images are scaled down to fit in this box.
     */
    public record Media(
        @DefaultValue("learning-images") String imageBucket,
        @DefaultValue("learning-audio") String audioBucket,
        @DefaultValue("800") int imageMaxWidth,
        @DefaultValue("600") int imageMaxHeight,
        @DefaultValue("5MB") DataSize audioMaxSize
    ) {}
}
