package com.lostark.lostark.service.users;

import com.lostark.lostark.model.dto.users.SignupUser;
import jakarta.servlet.http.HttpSession;

public interface UserService {
    /**
     * 유저 회원가입
     * @param user
     */
    void signup(SignupUser user, HttpSession session);

    boolean checkUserIdExists(String userId);

    void sendVerificationEmail(String email, HttpSession session);

    boolean verifyEmail(String email, String code, HttpSession session);
}
