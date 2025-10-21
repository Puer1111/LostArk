package com.lostark.lostark.service.users;

import com.lostark.lostark.model.dto.users.SignupUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.userdetails.UserDetailsService;

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

}
