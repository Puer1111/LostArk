package com.lostark.lostark.service.users;

import com.lostark.lostark.model.dto.users.SignupUser;
import com.lostark.lostark.model.entity.users.User;
import com.lostark.lostark.model.entity.users.UserRole;
import com.lostark.lostark.model.entity.users.UserStatus;
import com.lostark.lostark.model.repository.users.UserRepository;
import com.lostark.lostark.service.email.EmailService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // PasswordEncoder 주입
    private final EmailService emailService;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public void signup(SignupUser user, HttpSession session) {
        String isVerified = redisTemplate.opsForValue().get("email_verified:" + user.getUserEmail());

        if (!"true".equals(isVerified)) {
            throw new RuntimeException("이메일 인증이 완료되지 않았거나 만료되었습니다.");
        }

        // 비밀번호 암호화 -> user 에도 적용
        String encodedPassword = passwordEncoder.encode(user.getUserPassword());
        User newUser = user.toEntity(encodedPassword);
        userRepository.save(newUser);

        // Redis에서 이메일 인증 완료 상태 제거
        redisTemplate.delete("email_verified:" + user.getUserEmail());
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
        // Redis에 5분(300초) 동안 인증 코드 저장
        redisTemplate.opsForValue().set("email_code:" + email, code, 5, java.util.concurrent.TimeUnit.MINUTES);

        emailService.sendVerificationEmail(email, code);
    }

    @Override
    public boolean verifyEmail(String email, String code, HttpSession session) {
        String savedCode = redisTemplate.opsForValue().get("email_code:" + email);

        if (savedCode != null && savedCode.equals(code)) {
            // 인증 성공 시 인증 코드 삭제 및 인증 완료 상태 15분간 Redis 저장
            redisTemplate.delete("email_code:" + email);
            redisTemplate.opsForValue().set("email_verified:" + email, "true", 15, java.util.concurrent.TimeUnit.MINUTES);
            return true;
        }
        return false;
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getUserRole().name()));

        return new org.springframework.security.core.userdetails.User(user.getUserId(), user.getUserPassword(), authorities);
    }

    @Override
    @Transactional
    public User processKakaoUser(HashMap<String, Object> userInfo) {
        long kakaoId = Long.parseLong(userInfo.get("id").toString());
        User user = userRepository.findByKakaoId(kakaoId).orElse(null);

        if (user == null) { // New user
            // Safely extract nickname
            String nickName = "user_" + kakaoId; // Default nickname
            if (userInfo.get("kakao_account") != null) {
                HashMap<String, Object> kakaoAccount = (HashMap<String, Object>) userInfo.get("kakao_account");
                if (kakaoAccount.get("profile") != null) {
                    HashMap<String, Object> profile = (HashMap<String, Object>) kakaoAccount.get("profile");
                    if (profile.get("nickname") != null) {
                        nickName = profile.get("nickname").toString();
                    }
                }
            }

            // Handle nickname duplication
            while (userRepository.existsByUserNickName(nickName)) {
                String randomNumber = String.valueOf((int)(Math.random() * 10000));
                nickName = nickName + "#" + randomNumber;
            }

            // Safely extract email
            String email = "kakao_" + kakaoId + "@kakao.com"; // Placeholder email
            if (userInfo.get("kakao_account") != null) {
                HashMap<String, Object> kakaoAccount = (HashMap<String, Object>) userInfo.get("kakao_account");
                if (kakaoAccount.get("email") != null) {
                    email = kakaoAccount.get("email").toString();
                }
            }

            String userId = "kakao_" + kakaoId;
            String randomPassword = UUID.randomUUID().toString();
            String encodedPassword = passwordEncoder.encode(randomPassword);

            user = User.builder()
                    .userId(userId)
                    .userPassword(encodedPassword)
                    .userEmail(email)
                    .userNickName(nickName)
                    .kakaoId(kakaoId)
                    .userRole(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(user);
        }

        return user;
    }
}
