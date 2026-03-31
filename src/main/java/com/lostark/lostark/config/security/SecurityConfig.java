package com.lostark.lostark.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 보호 비활성화 (API 서버 등에서는 비활성화, 웹에서는 필요에 따라 설정)
                .csrf(csrf -> csrf.disable())

                // HTTP 요청에 대한 인가 설정
                /**
                 * 특정 조건달린 url 은 requestMatchers , 그 외에는 anyRequest
                 */
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

                // 폼 기반 로그인 설정
                .formLogin(form -> form
                        // 커스텀 로그인 페이지 경로 (나중에 만들어야 함)
                        .loginPage("/users/login")

                        // 로그인 성공 시 이동할 기본 URL
                        .defaultSuccessUrl("/", true)
                        // 로그인 페이지는 누구나 접근 가능
                        .permitAll()
                );

                // 로그아웃 설정
//                .logout(logout -> logout
//                        // 로그아웃 처리 URL
//                        .logoutUrl("/users/logout")
//                        // 로그아웃 성공 시 이동할 URL
//                        .logoutSuccessUrl("/")
//                );

        return http.build();
    }

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder 빈을 등록합니다.
     * BCrypt 알고리즘을 사용합니다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
