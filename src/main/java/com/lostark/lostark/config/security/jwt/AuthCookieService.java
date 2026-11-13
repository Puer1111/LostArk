package com.lostark.lostark.config.security.jwt;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieService {

    @Value("${app.security.cookie-secure:true}")
    private boolean secure;

    public void addAccessToken(HttpServletResponse response, String token, long maxAgeSeconds) {
        addCookie(response, "accessToken", token, "/", maxAgeSeconds);
    }

    public void addRefreshToken(HttpServletResponse response, String token, long maxAgeSeconds) {
        addCookie(response, "refreshToken", token, "/users", maxAgeSeconds);
    }

    public void clearAuthCookies(HttpServletResponse response) {
        addCookie(response, "accessToken", "", "/", 0);
        addCookie(response, "refreshToken", "", "/users", 0);
    }

    private void addCookie(HttpServletResponse response, String name, String value, String path, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(path)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
