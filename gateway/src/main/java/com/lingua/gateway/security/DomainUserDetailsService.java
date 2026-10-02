package com.lingua.gateway.security;

import com.lingua.gateway.domain.User;
import com.lingua.gateway.repository.UserRepository;
import java.util.Collection;
import java.util.Locale;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Authenticate a user from the database, by login or by email.
 */
@Component
public class DomainUserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;

    public DomainUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        String key = username.toLowerCase(Locale.ROOT);
        // A login cannot contain '@' (see RegisterVM), so this is unambiguous
        Mono<User> user = key.contains("@") ? userRepository.findByEmail(key) : userRepository.findByLogin(key);
        return user.flatMap(u ->
            userRepository
                .findAuthorities(u.getId())
                .map(SimpleGrantedAuthority::new)
                .collectList()
                .map(authorities -> new UserWithId(u, authorities))
        );
    }

    public static class UserWithId extends org.springframework.security.core.userdetails.User {

        private final Long id;

        public UserWithId(User user, Collection<? extends GrantedAuthority> authorities) {
            super(user.getLogin(), user.getPasswordHash(), user.isActivated(), true, true, true, authorities);
            this.id = user.getId();
        }

        public Long getId() {
            return id;
        }
    }
}
