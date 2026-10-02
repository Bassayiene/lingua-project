package com.lingua.gateway.web.rest;

import static com.lingua.gateway.security.SecurityUtils.AUTHORITIES_CLAIM;
import static com.lingua.gateway.security.SecurityUtils.JWT_ALGORITHM;
import static com.lingua.gateway.security.SecurityUtils.USER_ID_CLAIM;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lingua.gateway.config.ApplicationProperties;
import com.lingua.gateway.security.DomainUserDetailsService.UserWithId;
import com.lingua.gateway.web.rest.vm.LoginVM;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping("/api")
public class AuthenticateController {

    private final JwtEncoder jwtEncoder;

    private final ReactiveAuthenticationManager authenticationManager;

    private final ApplicationProperties.Jwt jwtProperties;

    public AuthenticateController(
        JwtEncoder jwtEncoder,
        ReactiveAuthenticationManager authenticationManager,
        ApplicationProperties properties
    ) {
        this.jwtEncoder = jwtEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtProperties = properties.security().jwt();
    }

    /**
     * {@code POST /authenticate} : exchange a login (or email) and password for a JWT.
     *
     * @return the token with status {@code 200 (OK)}, or {@code 401 (Unauthorized)} if the credentials are wrong.
     */
    @PostMapping("/authenticate")
    public Mono<ResponseEntity<JWTToken>> authorize(@Valid @RequestBody LoginVM loginVM) {
        return authenticationManager
            .authenticate(new UsernamePasswordAuthenticationToken(loginVM.username(), loginVM.password()))
            // Same answer whatever the cause, so the response does not reveal whether the account exists
            .onErrorMap(AuthenticationException.class, e -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides"))
            .map(authentication -> {
                String jwt = createToken(authentication, loginVM.rememberMe());
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setBearerAuth(jwt);
                return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
            });
    }

    private String createToken(Authentication authentication, boolean rememberMe) {
        String authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(" "));

        Instant now = Instant.now();
        long validity = rememberMe ? jwtProperties.tokenValidityInSecondsForRememberMe() : jwtProperties.tokenValidityInSeconds();

        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(now.plus(validity, ChronoUnit.SECONDS))
            .subject(authentication.getName())
            .claim(AUTHORITIES_CLAIM, authorities);
        if (authentication.getPrincipal() instanceof UserWithId user) {
            builder.claim(USER_ID_CLAIM, user.getId());
        }

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, builder.build())).getTokenValue();
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    public record JWTToken(@JsonProperty("id_token") String idToken) {}
}
