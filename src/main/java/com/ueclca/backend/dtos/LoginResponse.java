package com.ueclca.backend.dtos;

public record LoginResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {
}
