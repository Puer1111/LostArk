package com.lostark.lostark.service.admin;

import com.lostark.lostark.model.dto.admin.AdminUserResponseDto;
import com.lostark.lostark.model.entity.users.User;
import com.lostark.lostark.model.entity.users.UserRole;
import com.lostark.lostark.model.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    /**
     * 전체 유저 목록 조회
     */
    @Transactional(readOnly = true)
    public List<AdminUserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(AdminUserResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 유저 역할(Role) 변경 (USER <-> ADMIN)
     */
    @Transactional
    public AdminUserResponseDto updateUserRole(String userId, UserRole newRole) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));

        user.update(newRole, null);
        log.info("Admin updated user role: userId={}, newRole={}", userId, newRole);
        return AdminUserResponseDto.from(user);
    }
}
