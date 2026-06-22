package com.lostark.lostark.service.users;

import com.lostark.lostark.model.dto.users.SignupUser;
import com.lostark.lostark.model.entity.users.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.HashMap;

public interface UserService extends UserDetailsService {
    /**
     * 유저 회원가입
     * @param user
     */
    void signup(SignupUser user, HttpSession session);

    /**
     * DB에 userId 존재하는지 체크
     * @param userId
     * @return
     */
    boolean checkUserIdExists(String userId);

    /**
     * 이메일 검증 전송
     * @param email
     * @param session
     */
    void sendVerificationEmail(String email, HttpSession session);

    /**
     * 이메일 검증
     * @param email
     * @param code
     * @param session
     * @return
     */
    boolean verifyEmail(String email, String code, HttpSession session);

    /**
     * 카카오 간편 로그인
     * @param userInfo
     * @return User 엔티티
     */
    User processKakaoUser(HashMap<String, Object> userInfo);
}
