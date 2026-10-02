package com.lingua.gateway.service;

import com.lingua.gateway.domain.User;
import com.lingua.gateway.repository.UserRepository;
import com.lingua.gateway.security.AuthoritiesConstants;
import com.lingua.gateway.service.dto.UserDTO;
import com.lingua.gateway.web.rest.errors.EmailAlreadyUsedException;
import com.lingua.gateway.web.rest.errors.LoginAlreadyUsedException;
import com.lingua.gateway.web.rest.vm.RegisterVM;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service class for managing users.
 */
@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Create a learner account, usable immediately.
     */
    @Transactional
    public Mono<UserDTO> register(RegisterVM vm) {
        String login = vm.login().toLowerCase(Locale.ROOT);
        String email = vm.email().toLowerCase(Locale.ROOT);
        return userRepository
            .findByLogin(login)
            .flatMap(existing -> Mono.<User>error(new LoginAlreadyUsedException()))
            .switchIfEmpty(
                Mono.defer(() -> userRepository.findByEmail(email).flatMap(existing -> Mono.<User>error(new EmailAlreadyUsedException())))
            )
            .switchIfEmpty(
                Mono.defer(() ->
                    createUser(login, email, vm.password(), vm.firstName(), vm.lastName(), List.of(AuthoritiesConstants.USER))
                )
            )
            .flatMap(this::toDto);
    }

    @Transactional
    public Mono<User> createUser(
        String login,
        String email,
        String rawPassword,
        String firstName,
        String lastName,
        List<String> authorities
    ) {
        User user = new User();
        user.setLogin(login);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setActivated(true);
        user.setCreatedAt(Instant.now());
        return userRepository
            .save(user)
            .flatMap(saved ->
                Flux.fromIterable(authorities)
                    .concatMap(authority -> userRepository.addAuthority(saved.getId(), authority))
                    .then(Mono.just(saved))
            )
            .doOnNext(saved -> LOG.debug("Created user {} with authorities {}", saved.getLogin(), authorities));
    }

    @Transactional(readOnly = true)
    public Mono<UserDTO> findByLogin(String login) {
        return userRepository.findByLogin(login).flatMap(this::toDto);
    }

    @Transactional(readOnly = true)
    public Flux<UserDTO> findAll(Pageable pageable) {
        return userRepository.findAllBy(pageable).concatMap(this::toDto);
    }

    @Transactional(readOnly = true)
    public Mono<Long> count() {
        return userRepository.count();
    }

    private Mono<UserDTO> toDto(User user) {
        return userRepository
            .findAuthorities(user.getId())
            .collectList()
            .map(authorities ->
                new UserDTO(
                    user.getId(),
                    user.getLogin(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.isActivated(),
                    user.getCreatedAt(),
                    authorities
                )
            );
    }
}
