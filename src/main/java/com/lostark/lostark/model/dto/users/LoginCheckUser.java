package com.lostark.lostark.model.dto.users;

import com.lostark.lostark.model.entity.users.User;
import lombok.Data;

@Data

public class LoginCheckUser {
    private String userId;
    private String userPassword;


    public User toEntity(String encodedPassword) {
        return User.builder()
                .userId(userId)
                .userPassword(userPassword) // 암호화된 비밀번호 사용
                .build();
    }
}
