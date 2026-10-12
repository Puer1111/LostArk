package com.lostark.lostark.model.dto.admin;

import com.lostark.lostark.model.entity.users.User;
import com.lostark.lostark.model.entity.users.UserRole;
import com.lostark.lostark.model.entity.users.UserStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponseDto {
    private Long userNo;
    private String userId;
    private String userNickName;
    private String userEmail;
    private UserRole userRole;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public static AdminUserResponseDto from(User user) {
        return AdminUserResponseDto.builder()
                .userNo(user.getUserNo())
                .userId(user.getUserId())
                .userNickName(user.getUserNickName())
                .userEmail(user.getUserEmail())
                .userRole(user.getUserRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
