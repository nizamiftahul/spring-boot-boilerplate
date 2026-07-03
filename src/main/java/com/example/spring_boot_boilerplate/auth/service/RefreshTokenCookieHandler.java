package com.example.spring_boot_boilerplate.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface RefreshTokenCookieHandler {
    /**
     * Write the refresh token in the HTTP response as a cookie.
     *
     * @param response     the HTTP response
     * @param refreshToken the refresh token to write
     */
    void write(HttpServletResponse response, String refreshToken);

    /**
     * Delete the refresh token cookie in the HTTP response.
     *
     * @param response the HTTP response
     */
    void delete(HttpServletResponse response);

    /**
     * Extract the refresh token from the cookie in the HTTP request.
     *
     * @param request the HTTP request
     * @return the refresh token if present, null otherwise
     */
    String extract(HttpServletRequest request);
}
