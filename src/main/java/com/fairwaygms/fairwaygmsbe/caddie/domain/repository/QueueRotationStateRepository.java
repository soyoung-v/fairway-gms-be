package com.fairwaygms.fairwaygmsbe.caddie.domain.repository;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.QueueRotationState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QueueRotationStateRepository extends JpaRepository<QueueRotationState, Long> {

    Optional<QueueRotationState> findByGolfCourse_IdAndCaddieGroup_Id(Long golfCourseId, Long caddieGroupId);

    // 골프장의 전체 그룹 rotation state — 큐 초기화 시 한 번에 조회
    List<QueueRotationState> findByGolfCourse_Id(Long golfCourseId);
}
