package com.fairwaygms.fairwaygmsbe.caddie.domain.repository;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieGroup;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieGroupAssignmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CaddieGroupRepository extends JpaRepository<CaddieGroup, Long> {

    List<CaddieGroup> findByGolfCourse_IdAndIsDeletedFalseOrderByAssignmentTypeAscNameAsc(Long golfCourseId);

    Optional<CaddieGroup> findByGolfCourse_IdAndAssignmentTypeAndIsDeletedFalse(
            Long golfCourseId, CaddieGroupAssignmentType assignmentType);

    boolean existsByGolfCourse_IdAndIsDeletedFalse(Long golfCourseId);
}
