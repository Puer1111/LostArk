package com.lostark.lostark.controller.users;

import com.lostark.lostark.config.security.jwt.JwtTokenProvider;
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
import java.util.concurrent.TimeUnit;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final KakaoApi kakaoApi;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
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
            Cookie accessCookie = new Cookie("accessToken", accessToken);
            accessCookie.setHttpOnly(true);
            accessCookie.setSecure(false); // 개발 환경이므로 false, 프로덕션에서는 true 권장
            accessCookie.setPath("/");
            accessCookie.setMaxAge(60 * 30); // 30분 유지
            response.addCookie(accessCookie);

            Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(false);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(60 * 60 * 24 * 7); // 7일 유지
            response.addCookie(refreshCookie);

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
        Cookie accessCookie = new Cookie("accessToken", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(60 * 30); // 30분
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(60 * 60 * 24 * 7); // 7일
        response.addCookie(refreshCookie);

        return "redirect:/"; // 메인 페이지로 리다이렉트
    }

    @GetMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1. JWT 쿠키에서 Access Token 추출 및 Redis 블랙리스트 등록
        String accessToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            accessToken = Arrays.stream(cookies)
                    .filter(c -> "accessToken".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            String userId = jwtTokenProvider.getUserId(accessToken);
            // Redis에서 Refresh Token 삭제
            stringRedisTemplate.delete("refreshToken:" + userId);

            // Access Token 블랙리스트 등록 (남은 유효시간만큼)
            long remainingTime = jwtTokenProvider.getRemainingTime(accessToken);
            if (remainingTime > 0) {
                stringRedisTemplate.opsForValue().set(
                        "blacklist:" + accessToken,
                        "logout",
                        remainingTime,
                        TimeUnit.MILLISECONDS
                );
            }
        }

        // 2. JWT 쿠키 삭제
        Cookie accessCookie = new Cookie("accessToken", null);
        accessCookie.setPath("/");
        accessCookie.setHttpOnly(true);
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setPath("/");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);

        // 3. 카카오 로그아웃 또는 일반 로그아웃 처리
        try {
            String kakaoLogoutUrl = kakaoApi.getLogoutUrl();
            log.info("Redirecting to Kakao Logout URL: {}", kakaoLogoutUrl);
            response.sendRedirect(kakaoLogoutUrl);
        } catch (Exception e) {
            log.warn("Kakao logout failed or not configured, redirecting to home");
            response.sendRedirect("/");
        }
    }
}
