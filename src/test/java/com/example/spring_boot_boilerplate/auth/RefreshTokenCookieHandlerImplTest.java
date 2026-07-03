package com.example.spring_boot_boilerplate.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.example.spring_boot_boilerplate.auth.service.impl.RefreshTokenCookieHandlerImpl;

import jakarta.servlet.http.Cookie;

class RefreshTokenCookieHandlerImplTest {

    private static final long EXP_MS = 7 * 24 * 60 * 60 * 1000L; // example

    @Test
    void write_setsCookie_devProfile() {
        RefreshTokenCookieHandlerImpl handler = new RefreshTokenCookieHandlerImpl(EXP_MS, "dev");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        handler.write(resp, "rt-123");
        String setCookie = resp.getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refreshToken=rt-123"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Max-Age=" + (EXP_MS / 1000)));
        assertTrue(setCookie.contains("SameSite=Lax"));
        assertFalse(setCookie.toLowerCase().contains("secure"));
    }

    @Test
    void write_setsCookie_prodProfile_secureStrict() {
        RefreshTokenCookieHandlerImpl handler = new RefreshTokenCookieHandlerImpl(EXP_MS, "prod");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        handler.write(resp, "rt-xyz");
        String setCookie = resp.getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("SameSite=Strict"));
        assertTrue(setCookie.toLowerCase().contains("secure"));
    }

    @Test
    void delete_clearsCookie() {
        RefreshTokenCookieHandlerImpl handler = new RefreshTokenCookieHandlerImpl(EXP_MS, "dev");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        handler.delete(resp);
        String setCookie = resp.getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refreshToken="));
        assertTrue(setCookie.contains("Max-Age=0"));
    }

    @Test
    void extract_fromCookie() {
        RefreshTokenCookieHandlerImpl handler = new RefreshTokenCookieHandlerImpl(EXP_MS, "dev");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("refreshToken", "token-from-cookie"));

        assertEquals("token-from-cookie", handler.extract(req));
    }

    @Test
    void extract_fromHeader_when_noCookie() {
        RefreshTokenCookieHandlerImpl handler = new RefreshTokenCookieHandlerImpl(EXP_MS, "dev");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer header-token");

        assertEquals("header-token", handler.extract(req));
    }

    @Test
    void extract_nullWhen_noCookieAndBadHeader() {
        RefreshTokenCookieHandlerImpl handler = new RefreshTokenCookieHandlerImpl(EXP_MS, "dev");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Token abc"); // not Bearer

        assertNull(handler.extract(req));
    }

    @Test
    void extract_handles_nullCookies() {
        RefreshTokenCookieHandlerImpl handler = new RefreshTokenCookieHandlerImpl(EXP_MS, "dev");
        MockHttpServletRequest req = new MockHttpServletRequest();
        // no cookies, no header
        assertNull(handler.extract(req));
    }
}