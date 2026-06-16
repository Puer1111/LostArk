package com.lostark.lostark.config.security;

import com.lostark.lostark.config.security.jwt.JwtAuthenticationFilter;
import com.lostark.lostark.config.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 보호 비활성화 (API 서버이므로 비활성화)
                .csrf(csrf -> csrf.disable())
                // JWT를 사용하므로 세션을 사용하지 않음
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // HTTP Basic 인증 비활성화
                .httpBasic(httpBasic -> httpBasic.disable())
                // Form Login 비활성화 (JWT 사용 시 일반적으로 비활성화)
                .formLogin(form -> form.disable())

                // HTTP 요청에 대한 인가 설정
                .authorizeHttpRequests(authz -> authz
                        // 홈페이지, 회원가입, CSS/JS 등 정적 리소스는 누구나 접근 가능
                        .requestMatchers("/",
                                "/css/**",
                                "/js/**",
                                "/img/**",
                                "/users/signup", // 유저 회원가입
                                "/users/check-id/{userId}", // 유저 아이디 중복확인
                                "/email/send-verification", // 이메일 인증 코드 발송
                                "/email/verify-code", // 이메일 인증 코드 확인
                                "/users/login", // 로그인
                                "/character/{characterName}", // 유저 검색
                                "/character/expedition/{characterName}", // 원정대 검색
                                "/users/kakao/callback", // 카카오 로그인 콜백
                                "/users/kakao/login", // 카카오 인가코드 페이지
                                "/users/logout",// 로그아웃
                                "/character/party-simulator",// 파티 시뮬레이터 페이지
                                "/character/api/**", // 캐릭터 간소화 검색 API 허용
                                "/market/**" // 경매장 관련 페이지
                        ).permitAll()

                        // "/admin/**" 경로는 ADMIN 권한을 가진 사용자만 접근 가능
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 그 외 모든 요청은 인증된 사용자만 접근 가능
                        .anyRequest().authenticated()
                )
                // JWT 필터를 UsernamePasswordAuthenticationFilter 전에 추가
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * AuthenticationManager 빈을 등록합니다. (로그인 컨트롤러 등에서 사용 가능)
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
