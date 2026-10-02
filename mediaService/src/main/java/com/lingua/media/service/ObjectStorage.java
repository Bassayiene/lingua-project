package com.lingua.media.service;

/**
 * Where the files are kept. Buckets are readable by anyone: files are served straight to browsers.
 */
public interface ObjectStorage {
    /**
     * Store an object, creating the bucket if needed.
     *
     * @return the public URL of the object.
     */
    String store(String bucket, String objectKey, byte[] content, String contentType);

    void remove(String bucket, String objectKey);
}
