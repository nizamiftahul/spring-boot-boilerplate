package com.nizamiftahul.spring_boilerplate.modules.user.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String username, @NotBlank String password) {
}
