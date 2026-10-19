package com.lostark.lostark.config.security;

import com.lostark.lostark.config.security.jwt.JwtAuthenticationFilter;
import com.lostark.lostark.config.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
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
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

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
                                "/uploads/**",
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
                                "/character/class", // 클래스 시너지 정보 페이지
                                "/character/api/**", // 캐릭터 간소화 검색 API 허용
                                "/market/**", // 경매장 관련 페이지
                                "/actuator/**", // 모니터링 주소
                                "/admin/**" // 관리 페이지 화면 및 조회 허용
                        ).permitAll()

                        // 그 외 모든 요청은 인증된 사용자만 접근 가능
                        .anyRequest().authenticated()
                )
                // 예외 처리: 인증(401)/인가(403) 실패 시 JSON 응답 반환
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"status\": 401, \"error\": \"UNAUTHORIZED\", \"message\": \"로그인이 만료되었거나 로그인이 필요합니다.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"status\": 403, \"error\": \"FORBIDDEN\", \"message\": \"해당 작업을 수행할 관리자(ADMIN) 권한이 없습니다.\"}");
                        })
                )
                // JWT 필터를 UsernamePasswordAuthenticationFilter 전에 추가
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, stringRedisTemplate), UsernamePasswordAuthenticationFilter.class);

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
