package com.ueclca.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ueclca.backend.dtos.PasswordChangeRequest;
import com.ueclca.backend.dtos.PasswordResetRequest;
import com.ueclca.backend.dtos.UserCreateRequest;
import com.ueclca.backend.dtos.UserUpdateRequest;
import com.ueclca.backend.exceptions.BadRequestException;
import com.ueclca.backend.exceptions.ConflictException;
import com.ueclca.backend.exceptions.ResourceNotFoundException;
import com.ueclca.backend.models.Role;
import com.ueclca.backend.models.User;
import com.ueclca.backend.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createNormalizesEmailAndEncodesPassword() {
        when(userRepository.existsByEmail("nuevo@ueclca.edu.ve")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.create(new UserCreateRequest(
                "  Nuevo@Ueclca.Edu.Ve  ", "password123", "  Nuevo Usuario  ", Role.EDITOR));

        assertEquals("nuevo@ueclca.edu.ve", user.getEmail());
        assertEquals("hash", user.getPasswordHash());
        assertEquals("Nuevo Usuario", user.getFullName());
        assertEquals(Role.EDITOR, user.getRole());
        assertTrue(user.isEnabled());
    }

    @Test
    void createWithExistingEmailThrowsConflict() {
        when(userRepository.existsByEmail("duplicado@ueclca.edu.ve")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.create(new UserCreateRequest(
                "duplicado@ueclca.edu.ve", "password123", "Duplicado", Role.EDITOR)));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateSelfCannotDisableOwnAccount() {
        User admin = user("admin@ueclca.edu.ve", Role.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(BadRequestException.class, () -> userService.update(1L,
                new UserUpdateRequest("Administrador", Role.ADMIN, false), "ADMIN@ueclca.edu.ve"));
    }

    @Test
    void updateSelfCannotChangeOwnRole() {
        User admin = user("admin@ueclca.edu.ve", Role.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(BadRequestException.class, () -> userService.update(1L,
                new UserUpdateRequest("Administrador", Role.EDITOR, true), "admin@ueclca.edu.ve"));
    }

    @Test
    void updateOtherUserAppliesChanges() {
        User editor = user("editor@ueclca.edu.ve", Role.EDITOR, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(editor));

        User updated = userService.update(2L,
                new UserUpdateRequest("Editor Renombrado", Role.ADMIN, false), "admin@ueclca.edu.ve");

        assertEquals("Editor Renombrado", updated.getFullName());
        assertEquals(Role.ADMIN, updated.getRole());
        assertEquals(false, updated.isEnabled());
    }

    @Test
    void changeOwnPasswordWithWrongCurrentPasswordThrowsBadRequest() {
        User user = user("user@ueclca.edu.ve", Role.EDITOR, true);
        when(userRepository.findByEmail("user@ueclca.edu.ve")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("incorrecta", "hash")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> userService.changeOwnPassword(
                "user@ueclca.edu.ve", new PasswordChangeRequest("incorrecta", "nuevaPassword123")));
    }

    @Test
    void changeOwnPasswordEncodesNewPassword() {
        User user = user("user@ueclca.edu.ve", Role.EDITOR, true);
        when(userRepository.findByEmail("user@ueclca.edu.ve")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("actual123", "hash")).thenReturn(true);
        when(passwordEncoder.encode("nuevaPassword123")).thenReturn("nuevo-hash");

        userService.changeOwnPassword("user@ueclca.edu.ve",
                new PasswordChangeRequest("actual123", "nuevaPassword123"));

        assertEquals("nuevo-hash", user.getPasswordHash());
    }

    @Test
    void resetPasswordForUnknownUserThrowsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.resetPassword(99L, new PasswordResetRequest("nuevaPassword123")));
    }

    private User user(String email, Role role, boolean enabled) {
        return User.builder()
                .email(email)
                .passwordHash("hash")
                .fullName("Usuario de prueba")
                .role(role)
                .enabled(enabled)
                .build();
    }
}
