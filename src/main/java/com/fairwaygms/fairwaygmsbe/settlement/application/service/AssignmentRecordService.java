package com.fairwaygms.fairwaygmsbe.settlement.application.service;

import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.AssignmentRecord;
import com.fairwaygms.fairwaygmsbe.settlement.domain.enums.CompletionType;
import com.fairwaygms.fairwaygmsbe.settlement.domain.repository.AssignmentRecordRepository;
import com.fairwaygms.fairwaygmsbe.settlement.exception.SettlementErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AssignmentRecordService {

    private final AssignmentRecordRepository assignmentRecordRepository;

    // AssignmentCompletedEvent 수신 시 호출 — 정산 집계 원본 데이터 저장
    @Transactional
    public AssignmentRecord createRecord(Long golfCourseId, Long assignmentId, Long caddieId,
                                         LocalDate playDate, CompletionType completionType,
                                         Integer playedHoleCount, BigDecimal feeAmount) {
        if (assignmentRecordRepository.findByAssignmentIdAndIsDeletedFalse(assignmentId).isPresent()) {
            throw new BusinessException(SettlementErrorCode.ASSIGNMENT_RECORD_ALREADY_EXISTS);
        }

        // settlement_year_month는 playDate 기준으로 자동 도출
        String yearMonth = playDate.getYear() + "-" + String.format("%02d", playDate.getMonthValue());

        AssignmentRecord record = AssignmentRecord.create(
                golfCourseId, assignmentId, caddieId,
                playDate, yearMonth, completionType, playedHoleCount, feeAmount);
        return assignmentRecordRepository.save(record);
    }
}
