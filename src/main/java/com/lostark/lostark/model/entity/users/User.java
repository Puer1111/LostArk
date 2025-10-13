package com.lostark.lostark.model.entity.users;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "`user`") // 'user'는 예약어일 수 있으므로 백틱으로 감싸줍니다.
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userNo")
    private Long userNo;

    @Column(name = "userId", unique = true, length = 50)
    private String userId;

    @Column(name = "userPassword", nullable = false, length = 100)
    private String userPassword;

    @Column(name = "userEmail", nullable = false, length = 100)
    private String userEmail;

    @Column(name = "userNickName", nullable = false, unique = true, length = 30)
    private String userNickName;

    @Enumerated(EnumType.STRING)
    @Column(name = "userRole", nullable = false)
    private UserRole userRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder
    public User(String userId, String userPassword, String userEmail, String userNickName) {
        this.userId = userId;
        this.userPassword = userPassword;
        this.userEmail = userEmail;
        this.userNickName = userNickName;
        this.userRole = UserRole.USER;
        this.status = UserStatus.ACTIVE;
    }
}