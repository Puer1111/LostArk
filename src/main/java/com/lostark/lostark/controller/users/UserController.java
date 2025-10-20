package com.lostark.lostark.controller.users;

import com.lostark.lostark.model.dto.users.SignupUser;
import com.lostark.lostark.service.users.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/signup")
    public String signupPage() {
        return "users/signup"; // 'views/' prefix 제외
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupUser user, HttpSession session) {
        try {
            userService.signup(user, session);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

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
}
