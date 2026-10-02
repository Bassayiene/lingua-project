package com.lingua.media.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * The object storage (MinIO) could not be reached or refused the operation.
 */
public class StorageException extends ResponseStatusException {

    public StorageException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
