package com.example.InternalControl.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String email,
        String role
) {}
