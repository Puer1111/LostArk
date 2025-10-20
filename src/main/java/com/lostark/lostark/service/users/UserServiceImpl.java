package com.lostark.lostark.service.users;

import com.lostark.lostark.model.dto.users.SignupUser;
import com.lostark.lostark.model.entity.users.User;
import com.lostark.lostark.model.repository.users.UserRepository;
import com.lostark.lostark.service.email.EmailService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // PasswordEncoder 주입
    private final EmailService emailService;


    @Override
    @Transactional
    public void signup(SignupUser user, HttpSession session) {
        Boolean isEmailVerified = (Boolean) session.getAttribute("isEmailVerified");
        String verifiedEmail = (String) session.getAttribute("verifiedEmail");

        if (isEmailVerified == null || !isEmailVerified || !user.getUserEmail().equals(verifiedEmail)) {
            throw new RuntimeException("이메일 인증이 완료되지 않았습니다.");
        }

        // 비밀번호 암호화 -> user 에도 적용
        String encodedPassword = passwordEncoder.encode(user.getUserPassword());
        User newUser = user.toEntity(encodedPassword);
        userRepository.save(newUser);

        // 세션에서 인증 정보 제거
        session.removeAttribute("isEmailVerified");
        session.removeAttribute("verifiedEmail");
        session.removeAttribute("verificationCode");
        session.removeAttribute("verificationCodeExpiry");
    }

    @Override
    public boolean checkUserIdExists(String userId) {
        return userRepository.existsByUserId(userId);
    }

    @Override
    @Transactional
    public void sendVerificationEmail(String email, HttpSession session) {
        if (userRepository.findByUserEmail(email).isPresent()) {
            throw new RuntimeException("이미 사용중인 이메일입니다.");
        }

        String code = generateVerificationCode();
        session.setAttribute("verificationCode", code);
        session.setAttribute("verificationCodeExpiry", LocalDateTime.now().plusMinutes(10));
        session.setAttribute("verifiedEmail", email);

        emailService.sendVerificationEmail(email, code);
    }

    @Override
    public boolean verifyEmail(String email, String code, HttpSession session) {
        String sessionCode = (String) session.getAttribute("verificationCode");
        LocalDateTime expiryTime = (LocalDateTime) session.getAttribute("verificationCodeExpiry");
        String sessionEmail = (String) session.getAttribute("verifiedEmail");

        if (sessionCode == null || expiryTime == null || sessionEmail == null) {
            return false;
        }

        if (email.equals(sessionEmail) && code.equals(sessionCode) && expiryTime.isAfter(LocalDateTime.now())) {
            session.setAttribute("isEmailVerified", true);
            return true;
        }
        return false;
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
