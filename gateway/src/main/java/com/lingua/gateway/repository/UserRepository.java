package com.lingua.gateway.repository;

import com.lingua.gateway.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Logins and emails are stored in lower case, so lookups are exact matches.
 */
@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {
    Mono<User> findByLogin(String login);

    Mono<User> findByEmail(String email);

    Flux<User> findAllBy(Pageable pageable);

    @Query("SELECT authority_name FROM user_authority WHERE user_id = :userId ORDER BY authority_name")
    Flux<String> findAuthorities(Long userId);

    @Modifying
    @Query("INSERT INTO user_authority (user_id, authority_name) VALUES (:userId, :authority)")
    Mono<Integer> addAuthority(Long userId, String authority);
}
