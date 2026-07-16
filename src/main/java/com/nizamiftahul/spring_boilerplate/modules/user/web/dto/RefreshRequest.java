package com.nizamiftahul.spring_boilerplate.modules.user.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {
}
