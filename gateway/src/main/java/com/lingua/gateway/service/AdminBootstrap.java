package com.lingua.gateway.service;

import com.lingua.gateway.config.ApplicationProperties;
import com.lingua.gateway.config.ApplicationProperties.BootstrapAdmin;
import com.lingua.gateway.repository.UserRepository;
import com.lingua.gateway.security.AuthoritiesConstants;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * Creates the admin account at startup when it does not exist yet.
 * The password is only read at creation: changing it in the configuration afterwards has no effect.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminBootstrap.class);

    private final ApplicationProperties properties;

    private final UserRepository userRepository;

    private final UserService userService;

    public AdminBootstrap(ApplicationProperties properties, UserRepository userRepository, UserService userService) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        BootstrapAdmin admin = properties.bootstrapAdmin();
        if (admin == null || !StringUtils.hasText(admin.login()) || !StringUtils.hasText(admin.password())) {
            LOG.info("No bootstrap admin configured");
            return;
        }
        String login = admin.login().toLowerCase(Locale.ROOT);
        userRepository
            .findByLogin(login)
            .hasElement()
            .flatMap(exists -> {
                if (exists) {
                    return Mono.empty();
                }
                return userService
                    .createUser(
                        login,
                        login + "@localhost",
                        admin.password(),
                        "Administrator",
                        null,
                        List.of(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN)
                    )
                    .doOnNext(created -> LOG.info("Admin account {} created", created.getLogin()));
            })
            .block();
    }
}
