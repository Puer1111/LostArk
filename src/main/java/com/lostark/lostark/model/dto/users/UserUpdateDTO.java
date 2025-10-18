package com.lostark.lostark.model.dto.users;

import com.lostark.lostark.model.entity.users.UserRole;
import com.lostark.lostark.model.entity.users.UserStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateDTO {
    private UserRole userRole;
    private UserStatus status;
}
