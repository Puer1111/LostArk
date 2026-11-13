package com.lostark.lostark.service.admin;

import com.lostark.lostark.model.dto.admin.AdminUserResponseDto;
import com.lostark.lostark.model.entity.users.User;
import com.lostark.lostark.model.entity.users.UserRole;
import com.lostark.lostark.model.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

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

        if (user.getUserRole() == UserRole.ADMIN
                && newRole != UserRole.ADMIN
                && userRepository.countByUserRole(UserRole.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot remove the last administrator.");
        }

        user.update(newRole, null);
        log.info("Admin updated user role: userId={}, newRole={}", userId, newRole);
        return AdminUserResponseDto.from(user);
    }

    @Transactional(readOnly = true)
    public boolean isFirstAdminBootstrapAvailable(String configuredToken) {
        return configuredToken != null
                && !configuredToken.isBlank()
                && !userRepository.existsByUserRole(UserRole.ADMIN);
    }

    @Transactional
    public AdminUserResponseDto bootstrapFirstAdmin(String userId, String suppliedToken, String configuredToken) {
        if (configuredToken == null || configuredToken.isBlank() || suppliedToken == null
                || !MessageDigest.isEqual(
                        configuredToken.getBytes(StandardCharsets.UTF_8),
                        suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid administrator bootstrap token.");
        }

        Integer lockAcquired = jdbcTemplate.queryForObject(
                "SELECT GET_LOCK(?, 5)", Integer.class, "loahub_first_admin_bootstrap");
        if (lockAcquired == null || lockAcquired != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Could not acquire administrator bootstrap lock.");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                jdbcTemplate.queryForObject(
                        "SELECT RELEASE_LOCK(?)", Integer.class, "loahub_first_admin_bootstrap");
            }
        });

        if (userRepository.existsByUserRole(UserRole.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An administrator already exists.");
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
        user.update(UserRole.ADMIN, null);
        return AdminUserResponseDto.from(user);
    }
}
