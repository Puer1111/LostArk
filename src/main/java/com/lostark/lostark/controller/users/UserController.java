package com.lostark.lostark.controller.users;

import com.lostark.lostark.model.dto.users.LoginCheckUser;
import com.lostark.lostark.model.dto.users.SignupUser;
import com.lostark.lostark.service.api.KakaoApi;
import com.lostark.lostark.service.users.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/kakao/login")
    public void kakaoLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect(kakaoApi.getAuthorizationCode());
    }

    // 카카오 로그인 콜백
    @GetMapping("/kakao/callback")
    public String kakaoCallback(@RequestParam("code") String code, HttpSession session) {
        String accessToken = kakaoApi.getAccessToken(code);
        HashMap<String, Object> userInfo = kakaoApi.getUserInfo(accessToken);

        // 사용자 정보 처리 (로그인 또는 회원가입)
        userService.processKakaoUser(userInfo, session);

        return "redirect:/"; // 메인 페이지로 리다이렉트
    }

    @GetMapping("/logout")
    public void logout(HttpSession session, HttpServletResponse response) throws IOException {
        String kakaoLogoutUrl = kakaoApi.getLogoutUrl();
        log.info("Redirecting to Kakao Logout URL: {}", kakaoLogoutUrl);
        session.invalidate();
        response.sendRedirect(kakaoLogoutUrl);
    }
}
