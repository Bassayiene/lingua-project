package com.lingua.gateway.web.rest.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class LoginAlreadyUsedException extends ResponseStatusException {

    public LoginAlreadyUsedException() {
        super(HttpStatus.BAD_REQUEST, "Ce login est déjà utilisé");
    }
}
