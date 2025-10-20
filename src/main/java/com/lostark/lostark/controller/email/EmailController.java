package com.lostark.lostark.controller.email;

import com.lostark.lostark.service.users.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/email")
public class EmailController {

    private final UserService userService;

    @PostMapping("/send-verification")
    public ResponseEntity<?> sendVerificationEmail(@RequestBody Map<String, String> payload, HttpSession session) {
        try {
            String email = payload.get("email");
            userService.sendVerificationEmail(email, session);
            return ResponseEntity.ok("인증 코드를 발송했습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> payload, HttpSession session) {
        try {
            String email = payload.get("email");
            String code = payload.get("code");
            boolean isVerified = userService.verifyEmail(email, code, session);
            if (isVerified) {
                return ResponseEntity.ok("이메일 인증에 성공했습니다.");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("인증 코드가 일치하지 않거나 만료되었습니다.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
