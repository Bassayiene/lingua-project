package com.lingua.learning.web.rest;

import com.lingua.learning.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class CurrentUser {

    private CurrentUser() {}

    /**
     * @return the login of the authenticated user; answers 401 if the request carries no user.
     */
    static String login() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
