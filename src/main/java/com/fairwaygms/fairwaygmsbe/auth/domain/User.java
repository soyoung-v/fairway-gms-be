package com.fairwaygms.fairwaygmsbe.auth.domain;

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

    // 사용자 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소속 골프장 ID
    @Column(name = "golf_course_id")
    private Long golfCourseId;

    // 로그인 식별 이메일
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    // 이메일 인증 완료 여부
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    // BCrypt 비밀번호 해시
    @Column(name = "password_hash")
    private String passwordHash;

    // 사용자 이름
    @Column(nullable = false, length = 100)
    private String name;

    // 연락처
    @Column(length = 30)
    private String phone;

    // 사용자 권한
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    // 계정 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status = UserStatus.PENDING;

    // 가입 승인 처리자 ID
    @Column(name = "approved_by")
    private Long approvedBy;

    // 가입 승인 처리 시각
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // 마지막 로그인 시각
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // 소프트 삭제 여부
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // 소프트 삭제 시각
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

    // 승인 완료 계정으로 전환
    public void approve(Long approvedBy) {
        this.status = UserStatus.ACTIVE;
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
    }

    // 가입 요청 거절
    public void reject(Long approvedBy) {
        this.status = UserStatus.REJECTED;
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
    }

    // 로그인 성공 시각 갱신
    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    // 비밀번호 변경 — 반드시 BCrypt 해시된 값을 전달해야 한다.
    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }
}
