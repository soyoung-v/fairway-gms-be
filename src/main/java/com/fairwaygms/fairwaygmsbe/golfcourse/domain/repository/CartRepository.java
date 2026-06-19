package com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository;

import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Cart;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.enums.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 카트 조회/저장 인터페이스
public interface CartRepository extends JpaRepository<Cart, Long> {

    // 특정 골프장의 삭제되지 않은 전체 카트 목록 조회
    List<Cart> findAllByGolfCourseAndIsDeletedFalse(GolfCourse golfCourse);

    // 특정 골프장의 삭제되지 않은 카트 목록을 상태로 필터링하여 조회
    List<Cart> findAllByGolfCourseAndStatusAndIsDeletedFalse(GolfCourse golfCourse, CartStatus status);

    // 삭제되지 않은 카트 단건 조회
    Optional<Cart> findByIdAndIsDeletedFalse(Long id);

    // 같은 골프장 내 동일 카트 번호 중복 확인
    boolean existsByGolfCourseAndCartNumberAndIsDeletedFalse(GolfCourse golfCourse, String cartNumber);
}
