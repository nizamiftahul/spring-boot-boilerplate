package com.nizamiftahul.spring_boilerplate.modules.sample.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateSampleRequest(@NotBlank String name, String description) {
}
