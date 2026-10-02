package com.lingua.gateway.web.rest;

import com.lingua.gateway.service.UserService;
import com.lingua.gateway.service.dto.UserDTO;
import com.lingua.gateway.web.rest.vm.RegisterVM;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
public class AccountResource {

    private static final Logger LOG = LoggerFactory.getLogger(AccountResource.class);

    private final UserService userService;

    public AccountResource(UserService userService) {
        this.userService = userService;
    }

    /**
     * {@code POST /register} : create a learner account.
     *
     * @return the created account with status {@code 201 (Created)}, or {@code 400 (Bad Request)}
     *         if the login or the email is already used.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserDTO> register(@Valid @RequestBody RegisterVM registerVM) {
        LOG.debug("REST request to register {}", registerVM.login());
        return userService.register(registerVM);
    }

    /**
     * {@code GET /account} : get the current user.
     */
    @GetMapping("/account")
    public Mono<UserDTO> getAccount(@AuthenticationPrincipal Jwt jwt) {
        return userService
            .findByLogin(jwt.getSubject())
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Compte introuvable")));
    }
}
