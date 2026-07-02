package com.fairwaygms.fairwaygmsbe.auth.domain.repository;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.enums.UserStatus;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndIsDeletedFalse(String email);
    Optional<User> findByIdAndIsDeletedFalse(Long id);
    boolean existsByRoleAndStatusAndIsDeletedFalse(UserRole role, UserStatus status);
    boolean existsByEmailAndIsDeletedFalse(String email);
    List<User> findByRoleAndStatusAndIsDeletedFalse(UserRole role, UserStatus status);
    List<User> findByStatusAndIsDeletedFalseOrderByCreatedAtAsc(UserStatus status);
    List<User> findByIsDeletedFalseOrderByCreatedAtAsc();
    List<User> findByRoleAndStatusAndGolfCourseIdAndIsDeletedFalseOrderByCreatedAtAsc(UserRole role, UserStatus status, Long golfCourseId);
}
