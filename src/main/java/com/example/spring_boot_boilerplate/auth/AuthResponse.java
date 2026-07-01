package com.example.spring_boot_boilerplate.auth;

/**
 * Authentication Response DTO.
 * 
 * Contains the access token returned to client.
 * Refresh token is sent via HttpOnly cookie separately.
 */
public class AuthResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private String username;

    public AuthResponse() {}

    public AuthResponse(String accessToken, String username) {
        this.accessToken = accessToken;
        this.username = username;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
