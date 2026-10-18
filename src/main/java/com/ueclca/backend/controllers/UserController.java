package com.ueclca.backend.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ueclca.backend.dtos.PasswordResetRequest;
import com.ueclca.backend.dtos.UserCreateRequest;
import com.ueclca.backend.dtos.UserResponse;
import com.ueclca.backend.dtos.UserUpdateRequest;
import com.ueclca.backend.services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponse> list() {
        return userService.list().stream()
                .map(UserResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return UserResponse.from(userService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return UserResponse.from(userService.create(request));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(userService.update(id, request, jwt.getSubject()));
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(id, request);
    }
}
