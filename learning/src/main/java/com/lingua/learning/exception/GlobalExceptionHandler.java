package com.lingua.learning.exception;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Renders errors as RFC 7807 problem details. BadRequestException, NotFoundException and
 * ConflictException are ResponseStatusException and are handled by the parent class.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Unique or foreign key violation: duplicate name, or deleting something still in use.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        LOG.debug("Data integrity violation", ex);
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "Opération impossible : la valeur existe déjà ou l'élément est encore utilisé"
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        List<Map<String, String>> fieldErrors = ex
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
            .toList();
        ProblemDetail body = ex.getBody();
        body.setProperty("fieldErrors", fieldErrors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }
}
