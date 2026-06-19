package com.fairwaygms.fairwaygmsbe.auth.repository;

import com.fairwaygms.fairwaygmsbe.auth.domain.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.UserStatus;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일 로그인 대상 조회
    Optional<User> findByEmailAndIsDeletedFalse(String email);

    // 인증 사용자 ID 기준 조회
    Optional<User> findByIdAndIsDeletedFalse(Long id);

    // 특정 역할/상태 계정 존재 여부 확인
    boolean existsByRoleAndStatusAndIsDeletedFalse(UserRole role, UserStatus status);

    // 이메일 중복 가입 확인
    boolean existsByEmailAndIsDeletedFalse(String email);

    // 역할과 상태 기준 사용자 목록 조회
    List<User> findByRoleAndStatusAndIsDeletedFalse(UserRole role, UserStatus status);

    // 승인 대기 계정 목록 조회
    List<User> findByStatusAndIsDeletedFalseOrderByCreatedAtAsc(UserStatus status);
}
