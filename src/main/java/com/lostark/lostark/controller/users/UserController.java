package com.lostark.lostark.controller.users;

import com.lostark.lostark.config.security.jwt.JwtTokenProvider;
import com.lostark.lostark.config.security.jwt.AuthCookieService;
import com.lostark.lostark.model.dto.users.LoginCheckUser;
import com.lostark.lostark.model.dto.users.SignupUser;
import com.lostark.lostark.model.entity.users.User;
import com.lostark.lostark.model.repository.users.UserRepository;
import com.lostark.lostark.service.api.KakaoApi;
import com.lostark.lostark.service.users.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private static final DefaultRedisScript<Long> ROTATE_REFRESH_TOKEN_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then "
                    + "redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3]); return 1 "
                    + "else return 0 end",
            Long.class);
    private final UserService userService;
    private final KakaoApi kakaoApi;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthCookieService authCookieService;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @GetMapping("/signup")
    public String signupPage() {
        return "users/signup";
    }

    // 회원가입 페이지
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupUser user, HttpSession session) {
        try {
            userService.signup(user, session);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // 아이디 중복 확인
    @GetMapping("/check-id/{userId}")
    public ResponseEntity<?> checkUserId(@PathVariable String userId) {
        boolean isExists = userService.checkUserIdExists(userId);
        if (isExists) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("아이디가 존재 합니다.");
        } else {
            return ResponseEntity.ok("사용 가능한 아이디입니다.");
        }
    }

    // 로그인 페이지
    @GetMapping("/login")
    public String loginPage() {
        return "users/login";
    }

    // JWT 로그인 처리
    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestBody LoginCheckUser loginDto, HttpServletResponse response) {
        try {
            // 1. 아이디/비밀번호 인증
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getUserId(), loginDto.getUserPassword())
            );

            // 2. 인증 성공 시 유저 정보 조회
            User user = userRepository.findByUserId(loginDto.getUserId())
                    .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

            // 3. 이중 토큰(Access Token / Refresh Token) 발급
            String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getUserRole().name());
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

            // 4. Redis에 Refresh Token 저장 (7일 만료)
            stringRedisTemplate.opsForValue().set(
                    "refreshToken:" + user.getUserId(),
                    refreshToken,
                    jwtTokenProvider.getRefreshTokenValidityInMilliseconds(),
                    TimeUnit.MILLISECONDS
            );

            // 5. HttpOnly 쿠키 생성 및 설정 (XSS 방지)
            authCookieService.addAccessToken(response, accessToken, jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000);
        authCookieService.addRefreshToken(response, refreshToken, jwtTokenProvider.getRefreshTokenValidityInMilliseconds() / 1000);

            // 6. 응답 생성
            HashMap<String, String> result = new HashMap<>();
            result.put("userId", user.getUserId());
            result.put("role", user.getUserRole().name());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Login failed for user: {}", loginDto.getUserId(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인에 실패하였습니다. 아이디 또는 비밀번호를 확인해주세요.");
        }
    }

    @GetMapping("/kakao/login")
    public void loginWithKaKao(HttpServletResponse response) throws IOException {
        response.sendRedirect(kakaoApi.getAuthorizationCode());
    }

    // 카카오 로그인 콜백
    @GetMapping("/kakao/callback")
    public String CallbackToKaKao(@RequestParam("code") String code, HttpServletResponse response) {
        String kakaoAccessToken = kakaoApi.getAccessToken(code);
        HashMap<String, Object> userInfo = kakaoApi.getUserInfo(kakaoAccessToken);

        // 사용자 정보 처리 (로그인 또는 회원가입)
        User user = userService.processKakaoUser(userInfo);

        // 이중 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getUserRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        // Redis에 Refresh Token 저장 (7일 만료)
        stringRedisTemplate.opsForValue().set(
                "refreshToken:" + user.getUserId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenValidityInMilliseconds(),
                TimeUnit.MILLISECONDS
        );

        // HttpOnly 쿠키에 토큰 심기
        authCookieService.addAccessToken(response, accessToken, jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000);
        authCookieService.addRefreshToken(response, refreshToken, jwtTokenProvider.getRefreshTokenValidityInMilliseconds() / 1000);

        return "redirect:/"; // 메인 페이지로 리다이렉트
    }

    @PostMapping("/refresh")
    @ResponseBody
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        String refreshToken = getCookieValue(request, "refreshToken");
        if (refreshToken == null || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
            authCookieService.clearAuthCookies(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = jwtTokenProvider.getUserId(refreshToken);
        String redisKey = "refreshToken:" + userId;
        if (!refreshToken.equals(stringRedisTemplate.opsForValue().get(redisKey))) {
            authCookieService.clearAuthCookies(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByUserId(userId).orElse(null);
        if (user == null) {
            stringRedisTemplate.delete(redisKey);
            authCookieService.clearAuthCookies(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String access = jwtTokenProvider.createAccessToken(userId, user.getUserRole().name());
        String refresh = jwtTokenProvider.createRefreshToken(userId);
        Long rotated = stringRedisTemplate.execute(ROTATE_REFRESH_TOKEN_SCRIPT, List.of(redisKey), refreshToken,
                refresh, String.valueOf(jwtTokenProvider.getRefreshTokenValidityInMilliseconds()));
        if (!Long.valueOf(1L).equals(rotated)) {
            authCookieService.clearAuthCookies(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authCookieService.addAccessToken(response, access, jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000);
        authCookieService.addRefreshToken(response, refresh, jwtTokenProvider.getRefreshTokenValidityInMilliseconds() / 1000);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String accessToken = getCookieValue(request, "accessToken");
        String refreshToken = getCookieValue(request, "refreshToken");
        String userId = null;
        if (accessToken != null && jwtTokenProvider.validateAccessToken(accessToken)) {
            userId = jwtTokenProvider.getUserId(accessToken);
            long remainingTime = jwtTokenProvider.getRemainingTime(accessToken);
            if (remainingTime > 0) {
                stringRedisTemplate.opsForValue().set("blacklist:" + accessToken, "logout",
                        remainingTime, TimeUnit.MILLISECONDS);
            }
        } else if (refreshToken != null && jwtTokenProvider.validateRefreshToken(refreshToken)) {
            userId = jwtTokenProvider.getUserId(refreshToken);
        }
        if (userId != null) stringRedisTemplate.delete("refreshToken:" + userId);
        authCookieService.clearAuthCookies(response);

        try {
            String logoutUrl = kakaoApi.getLogoutUrl();
            if (logoutUrl != null && !logoutUrl.isBlank()) {
                response.sendRedirect(logoutUrl);
                return;
            }
        } catch (Exception exception) {
            log.warn("카카오 로그아웃 URL 생성에 실패했습니다.");
        }
        response.sendRedirect("/");
    }
    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies).filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue).findFirst().orElse(null);
    }
}
