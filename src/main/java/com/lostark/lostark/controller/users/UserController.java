package com.lostark.lostark.controller.users;

import com.lostark.lostark.config.security.jwt.JwtTokenProvider;
import com.lostark.lostark.model.dto.users.LoginCheckUser;
import com.lostark.lostark.model.dto.users.SignupUser;
import com.lostark.lostark.model.dto.users.TokenResponse;
import com.lostark.lostark.model.entity.users.User;
import com.lostark.lostark.model.repository.users.UserRepository;
import com.lostark.lostark.service.api.KakaoApi;
import com.lostark.lostark.service.users.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;


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

    @GetMapping("/signup")
    public String signupPage() {
        return "users/signup"; // 'views/' prefix 제외
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
        boolean isExists = userService.checkUserIdExists(userId); // 원래 메소드명 사용
        if (isExists) {
            // ID가 존재하면 409 Conflict 상태와 메시지 반환
            return ResponseEntity.status(HttpStatus.CONFLICT).body("아이디가 존재 합니다.");
        } else {
            // ID가 존재하지 않으면 200 OK 상태와 메시지 반환
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

            // 3. 토큰 발급
            String token = jwtTokenProvider.createToken(user.getUserId(), user.getUserRole().name());

            // 4. 쿠키 생성 및 설정 (HttpOnly)
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("accessToken", token);
            cookie.setHttpOnly(true); // JS에서 접근 불가 (XSS 방지)
            cookie.setSecure(false);  // HTTPS인 경우 true로 설정 (현재 개발 환경이므로 false)
            cookie.setPath("/");      // 모든 경로에서 전송
            cookie.setMaxAge(60 * 60 * 24); // 24시간 유지
            response.addCookie(cookie);

            // 5. 응답 생성 (토큰은 쿠키에 있으므로 유저 정보만 반환하거나 빈 응답 반환)
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
    public String CallbackToKaKao(@RequestParam("code") String code, HttpSession session) {
        String accessToken = kakaoApi.getAccessToken(code);
        HashMap<String, Object> userInfo = kakaoApi.getUserInfo(accessToken);

        // 사용자 정보 처리 (로그인 또는 회원가입)
        userService.processKakaoUser(userInfo, session);

        return "redirect:/"; // 메인 페이지로 리다이렉트
    }

    @GetMapping("/logout")
    public void logout(HttpSession session, HttpServletResponse response) throws IOException {
        // 1. JWT 쿠키 삭제
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("accessToken", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0); // 즉시 만료
        response.addCookie(cookie);

        // 2. 세션 무효화
        session.invalidate();

        // 3. 카카오 로그아웃 또는 일반 로그아웃 처리
        // 만약 카카오 로그인이었다면 카카오 로그아웃 URL로 이동, 아니면 홈으로
        // (현재는 항상 카카오 로그아웃 URL로 가는 구조라면 유지하거나 분기 처리 가능)
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
