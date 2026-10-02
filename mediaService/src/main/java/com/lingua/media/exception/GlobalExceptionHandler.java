package com.lingua.media.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Renders errors as RFC 7807 problem details, including the exceptions of this package
 * (they are ResponseStatusException) and uploads over the multipart size limit.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {}
