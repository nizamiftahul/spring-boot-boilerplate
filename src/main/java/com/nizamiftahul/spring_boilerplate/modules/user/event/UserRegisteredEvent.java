package com.nizamiftahul.spring_boilerplate.modules.user.event;

public record UserRegisteredEvent(Long userId, String username, String email) {
}
