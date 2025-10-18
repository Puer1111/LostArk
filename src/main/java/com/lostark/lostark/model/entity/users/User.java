package com.lostark.lostark.model.entity.users;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "`user`") // 'user'는 예약어일 수 있으므로 백틱으로 감싸줍니다.
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userNo")
    private Long userNo;

    @Column(name = "userId", unique = true, length = 50)
    private String userId;

    @Column(name = "userPassword", nullable = false, length = 255)
    private String userPassword;

    @Column(name = "userEmail", nullable = false, length = 50)
    private String userEmail;

    @Column(name = "userNickName", nullable = false, unique = true, length = 30)
    private String userNickName;

    @Enumerated(EnumType.STRING)
    @Column(name = "userRole", nullable = false)
    @Builder.Default
    private UserRole userRole = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    

    public void update(UserRole userRole, UserStatus status) {
        if (userRole != null) {
            this.userRole = userRole;
        }
        if (status != null) {
            this.status = status;
        }
    }
}