package com.fairwaygms.fairwaygmsbe.settlement.domain.repository;

import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.AssignmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentRecordRepository extends JpaRepository<AssignmentRecord, Long> {

    Optional<AssignmentRecord> findByAssignmentIdAndIsDeletedFalse(Long assignmentId);

    List<AssignmentRecord> findByGolfCourseIdAndSettlementYearMonthAndIsDeletedFalse(
            Long golfCourseId, String settlementYearMonth);
}
