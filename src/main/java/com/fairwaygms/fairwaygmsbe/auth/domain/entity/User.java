package com.fairwaygms.fairwaygmsbe.auth.domain.entity;

import com.fairwaygms.fairwaygmsbe.auth.domain.enums.UserStatus;
import com.fairwaygms.fairwaygmsbe.common.entity.BaseEntity;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
// 사용자 계정 마스터
@Table(
        name = "app_user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_app_user_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_app_user_role", columnList = "role"),
                @Index(name = "idx_app_user_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "golf_course_id")
    private Long golfCourseId;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status = UserStatus.PENDING;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 이메일 회원가입 계정 생성
    public static User createEmailUser(
            String email,
            String passwordHash,
            String name,
            String phone,
            UserRole role,
            Long golfCourseId
    ) {
        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.name = name;
        user.phone = phone;
        user.role = role;
        user.golfCourseId = golfCourseId;
        user.emailVerified = false;
        user.status = UserStatus.PENDING;
        user.isDeleted = false;
        return user;
    }

    // 초기 ADMIN 계정 생성
    public static User createInitialAdmin(String email, String passwordHash, String name) {
        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.name = name;
        user.role = UserRole.ADMIN;
        user.golfCourseId = null;
        user.emailVerified = true;
        user.status = UserStatus.ACTIVE;
        user.approvedAt = LocalDateTime.now();
        user.isDeleted = false;
        return user;
    }

    public void approve(Long approvedBy) {
        this.status = UserStatus.ACTIVE;
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject(Long approvedBy) {
        this.status = UserStatus.REJECTED;
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
    }

    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    // 비밀번호 변경 — 반드시 BCrypt 해시된 값을 전달해야 한다.
    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }
}
