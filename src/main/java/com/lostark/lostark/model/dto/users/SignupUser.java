package com.lostark.lostark.model.dto.users;

import com.lostark.lostark.model.entity.users.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupUser {

    @NotBlank(message = "아이디는 필수 입력 값입니다.")
    @Size(min = 4, max = 50, message = "아이디는 4자 이상 50자 이하로 입력해주세요.")
    private String userId;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    @Size(min = 8, max = 50, message = "비밀번호는 8자 이상 50 이하로 입력해주세요.")
    private String userPassword;

    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "이메일 형식에 맞지 않습니다.")
    @Size(max = 50, message = "이메일은 50자 이하로 입력해주세요.")
    private String userEmail;

    @NotBlank(message = "닉네임은 필수 입력 값입니다.")
    @Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하로 입력해주세요.")
    private String userNickName;

    public User toEntity(String encodedPassword) {
        return User.builder()
                .userId(userId)
                .userPassword(encodedPassword) // 암호화된 비밀번호 사용
                .userEmail(userEmail)
                .userNickName(userNickName)
                .build();
    }
}