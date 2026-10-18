package com.ueclca.backend.services;

import com.ueclca.backend.dtos.PasswordChangeRequest;
import com.ueclca.backend.dtos.PasswordResetRequest;
import com.ueclca.backend.dtos.UserCreateRequest;
import com.ueclca.backend.dtos.UserUpdateRequest;
import com.ueclca.backend.exceptions.BadRequestException;
import com.ueclca.backend.exceptions.ConflictException;
import com.ueclca.backend.exceptions.ResourceNotFoundException;
import com.ueclca.backend.models.User;
import com.ueclca.backend.repositories.UserRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User getByEmail(String email) {
        return userRepository
            .findByEmail(normalizeEmail(email))
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    public User getById(Long id) {
        return userRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    public List<User> list() {
        return userRepository.findAllByOrderByFullNameAsc();
    }

    @Transactional
    public User create(UserCreateRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Ya existe un usuario con ese correo");
        }

        return userRepository.save(
            User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .role(request.role())
                .enabled(true)
                .build()
        );
    }

    @Transactional
    public User update(Long id, UserUpdateRequest request, String currentUserEmail) {
        User user = getById(id);
        boolean self = user.getEmail().equals(normalizeEmail(currentUserEmail));

        if (self && !request.enabled()) {
            throw new BadRequestException("No puedes desactivar tu propia cuenta");
        }
        if (self && request.role() != user.getRole()) {
            throw new BadRequestException("No puedes cambiar tu propio rol");
        }

        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setEnabled(request.enabled());
        return user;
    }

    @Transactional
    public void resetPassword(Long id, PasswordResetRequest request) {
        User user = getById(id);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void changeOwnPassword(String email, PasswordChangeRequest request) {
        User user = getByEmail(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("La contraseña actual no es correcta");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
