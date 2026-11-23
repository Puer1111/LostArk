package com.lostark.lostark.config.security;

import com.lostark.lostark.config.security.jwt.JwtAuthenticationFilter;
import com.lostark.lostark.config.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository(
            @Value("${app.security.csrf-cookie-secure:false}") boolean secureCsrfCookie) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> cookie.secure(secureCsrfCookie).sameSite("Lax"));
        return repository;
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            CookieCsrfTokenRepository csrfTokenRepository) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/users/logout")
                        .csrfTokenRepository(csrfTokenRepository)
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(
                                "/",
                                "/css/**",
                                "/js/**",
                                "/img/**",
                                "/uploads/**",
                                "/users/signup",
                                "/users/check-id/{userId}",
                                "/users/login",
                                "/users/refresh",
                                "/users/logout",
                                "/email/send-verification",
                                "/email/verify-code",
                                "/character/{characterName}",
                                "/character/expedition/{characterName}",
                                "/users/kakao/callback",
                                "/users/kakao/login",
                                "/character/party-simulator",
                                "/character/class",
                                "/character/api/**",
                                "/market/**",
                                "/actuator/**",
                                "/admin/**"
                        ).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Authentication required.\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            if (exception instanceof CsrfException) {
                                response.getWriter().write("{\"status\":403,\"error\":\"CSRF_REJECTED\",\"message\":\"CSRF token is missing or invalid.\"}");
                            } else {
                                response.getWriter().write("{\"status\":403,\"error\":\"FORBIDDEN\",\"message\":\"Administrator permission is required.\"}");
                            }
                        }))
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, stringRedisTemplate),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new CsrfTokenMaterializationFilter(), CsrfFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
