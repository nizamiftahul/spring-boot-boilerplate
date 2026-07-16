package com.nizamiftahul.spring_boilerplate.modules.user.web.dto;

public record AuthResponse(String accessToken, String refreshToken, long expiresIn) {
}
