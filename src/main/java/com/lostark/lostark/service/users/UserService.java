package com.lostark.lostark.service.users;

import com.lostark.lostark.model.dto.users.SignupUser;

public interface UserService {
    /**
     * 유저 회원가입
     * @param user
     */
    void signup(SignupUser user);

    boolean checkUserIdExists(String userId);
}
