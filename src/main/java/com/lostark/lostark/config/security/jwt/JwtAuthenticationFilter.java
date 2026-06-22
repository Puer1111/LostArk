
package com.lostark.lostark.config.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Request에서 Access Token 추출
        String token = resolveToken(request);

        // 2. Access Token 블랙리스트 체크
        if (token != null) {
            String isBlacklisted = stringRedisTemplate.opsForValue().get("blacklist:" + token);
            if (isBlacklisted != null) {
                log.warn("Access Token is blacklisted: {}", token);
                filterChain.doFilter(request, response);
                return;
            }
        }

        // 3. Access Token 유효성 검사 및 인증 객체 설정
        if (token != null && jwtTokenProvider.validateToken(token)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } 
        // 4. Access Token이 없거나 만료되었을 때 Refresh Token을 이용한 자동 갱신 시도
        else {
            String refreshToken = resolveRefreshToken(request);
            if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
                String userId = jwtTokenProvider.getUserId(refreshToken);
                String redisRefreshToken = stringRedisTemplate.opsForValue().get("refreshToken:" + userId);

                // Redis에 저장된 Refresh Token과 일치하는지 확인
                if (refreshToken.equals(redisRefreshToken)) {
                    try {
                        Authentication authentication = jwtTokenProvider.getAuthentication(refreshToken);
                        String role = authentication.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .findFirst()
                                .orElse("ROLE_USER")
                                .replace("ROLE_", "");

                        // 새 Access Token 생성
                        String newAccessToken = jwtTokenProvider.createAccessToken(userId, role);

                        // 새 Access Token을 쿠키에 설정
                        Cookie cookie = new Cookie("accessToken", newAccessToken);
                        cookie.setHttpOnly(true);
                        cookie.setSecure(false); // 개발 환경이므로 false, 배포 환경에서는 true 권장
                        cookie.setPath("/");
                        cookie.setMaxAge(60 * 30); // 30분
                        response.addCookie(cookie);

                        // SecurityContext에 인증 객체 저장
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.info("Access Token successfully refreshed for user: {}", userId);
                    } catch (Exception e) {
                        log.error("Failed to auto refresh Access Token for user: {}", userId, e);
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    // Request Header 또는 Cookie 에서 Access Token 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(c -> "accessToken".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    // Cookie 에서 Refresh Token 추출
    private String resolveRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(c -> "refreshToken".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}
