package com.lingua.gateway.web.rest;

import com.lingua.gateway.service.UserService;
import com.lingua.gateway.service.dto.UserDTO;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for managing users. Reserved to administrators (see SecurityConfiguration).
 */
@RestController
@RequestMapping("/api/admin")
public class UserResource {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserService userService;

    public UserResource(UserService userService) {
        this.userService = userService;
    }

    /**
     * {@code GET /admin/users} : get a page of users, most recent first.
     * The total number of users is returned in the {@code X-Total-Count} header.
     */
    @GetMapping("/users")
    public Mono<ResponseEntity<List<UserDTO>>> getAllUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.DESC, "id")
        );
        return userService
            .count()
            .flatMap(total ->
                userService
                    .findAll(pageable)
                    .collectList()
                    .map(users -> ResponseEntity.ok().header("X-Total-Count", String.valueOf(total)).body(users))
            );
    }
}
