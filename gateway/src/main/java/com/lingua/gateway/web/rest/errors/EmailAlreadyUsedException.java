package com.lingua.gateway.web.rest.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class EmailAlreadyUsedException extends ResponseStatusException {

    public EmailAlreadyUsedException() {
        super(HttpStatus.BAD_REQUEST, "Cet email est déjà utilisé");
    }
}
