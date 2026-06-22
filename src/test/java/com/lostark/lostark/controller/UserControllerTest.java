package com.lostark.lostark.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostark.lostark.config.security.jwt.JwtTokenProvider;
import com.lostark.lostark.controller.users.UserController;
import com.lostark.lostark.model.dto.users.LoginCheckUser;
import com.lostark.lostark.model.entity.users.User;
import com.lostark.lostark.model.entity.users.UserRole;
import com.lostark.lostark.model.entity.users.UserStatus;
import com.lostark.lostark.model.repository.users.UserRepository;
import com.lostark.lostark.service.api.KakaoApi;
import com.lostark.lostark.service.users.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private KakaoApi kakaoApi;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId("testuser")
                .userPassword("hashedPassword")
                .userEmail("test@example.com")
                .userNickName("테스터")
                .userRole(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Test
    @DisplayName("일반 로그인 성공 시 이중 JWT 토큰을 쿠키에 설정하고 Redis에 Refresh Token을 저장한다")
    @WithMockUser
    void login_Success() throws Exception {
        // given
        LoginCheckUser loginDto = new LoginCheckUser();
        loginDto.setUserId("testuser");
        loginDto.setUserPassword("password123!");

        Authentication authentication = Mockito.mock(Authentication.class);
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(authentication);
        given(userRepository.findByUserId("testuser")).willReturn(Optional.of(testUser));
        given(jwtTokenProvider.createAccessToken("testuser", "USER")).willReturn("mockAccessToken");
        given(jwtTokenProvider.createRefreshToken("testuser")).willReturn("mockRefreshToken");
        given(jwtTokenProvider.getRefreshTokenValidityInMilliseconds()).willReturn(604800000L);

        // when
        mockMvc.perform(post("/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().value("accessToken", "mockAccessToken"))
                .andExpect(cookie().httpOnly("accessToken", true))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().value("refreshToken", "mockRefreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));

        // Redis 저장 로직 검증
        verify(valueOperations).set(
                eq("refreshToken:testuser"),
                eq("mockRefreshToken"),
                eq(604800000L),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("로그아웃 시 JWT 쿠키를 즉시 만료시키고 Redis에서 Refresh Token 삭제 및 Access Token을 블랙리스트에 등록한다")
    @WithMockUser
    void logout_Success() throws Exception {
        // given
        Cookie accessCookie = new Cookie("accessToken", "mockAccessToken");
        given(jwtTokenProvider.validateToken("mockAccessToken")).willReturn(true);
        given(jwtTokenProvider.getUserId("mockAccessToken")).willReturn("testuser");
        given(jwtTokenProvider.getRemainingTime("mockAccessToken")).willReturn(1800000L); // 30분 남음

        // when
        mockMvc.perform(get("/users/logout")
                        .cookie(accessCookie))
                // then
                .andExpect(status().is3xxRedirection()) // 카카오 로그아웃 URL 리다이렉트 기대
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().maxAge("refreshToken", 0));

        // Redis 블랙리스트 등록 및 삭제 검증
        verify(stringRedisTemplate).delete("refreshToken:testuser");
        verify(valueOperations).set(
                eq("blacklist:mockAccessToken"),
                eq("logout"),
                eq(1800000L),
                eq(TimeUnit.MILLISECONDS)
        );
    }
}
