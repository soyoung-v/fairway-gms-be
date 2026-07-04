package com.fairwaygms.fairwaygmsbe.settlement.application.service;

import com.fairwaygms.fairwaygmsbe.assignment.application.event.AssignmentCompletedEvent;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.enums.AssignmentStatus;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.ReservationTeamStatus;
import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.CaddieFeepolicy;
import com.fairwaygms.fairwaygmsbe.settlement.domain.enums.CompletionType;
import com.fairwaygms.fairwaygmsbe.settlement.exception.SettlementErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementEventListener {

    private final AssignmentRecordService assignmentRecordService;
    private final AssignmentRepository assignmentRepository;
    private final FeePolicyService feePolicyService;

    // 배정표 완료(라운딩 종료) → 정산 기록 생성
    // REQUIRES_NEW: AFTER_COMMIT 이후 별도 트랜잭션에서 실행
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAssignmentCompleted(AssignmentCompletedEvent event) {
        List<Assignment> assignments = assignmentRepository.findByGolfCourseAndDateAndStatus(
                event.getGolfCourseId(), event.getScheduleDate(), AssignmentStatus.COMPLETED);

        if (assignments.isEmpty()) return;

        // 캐디피 정책이 없으면 기록 자체를 남길 수 없으므로 경고만 남기고 종료
        CaddieFeepolicy policy;
        try {
            policy = feePolicyService.getPolicy(event.getGolfCourseId());
        } catch (BusinessException e) {
            log.warn("캐디피 정책 미설정 — 정산 기록 생략 (golfCourseId={})", event.getGolfCourseId());
            return;
        }

        for (Assignment a : assignments) {
            try {
                CompletionType completionType = resolveCompletionType(a.getReservationTeam().getStatus());
                int holes = resolveHoles(completionType);
                BigDecimal fee = computeFee(policy, completionType, holes, a.getIsHalfBack(),
                        a.getReservationTeam().getPlayerCount());

                assignmentRecordService.createRecord(
                        event.getGolfCourseId(), a.getId(),
                        a.getCaddie().getId(), event.getScheduleDate(),
                        completionType, holes, fee);
            } catch (BusinessException e) {
                // 이미 기록된 배정은 건너뜀 (재처리 안전)
                log.debug("정산 기록 중복 — assignmentId={}", a.getId());
            }
        }
    }

    private CompletionType resolveCompletionType(ReservationTeamStatus teamStatus) {
        return switch (teamStatus) {
            case COMPLETED -> CompletionType.NORMAL;
            case NO_SHOW -> CompletionType.NO_SHOW;
            case RAIN_CANCELLED -> CompletionType.RAIN_CANCELLED;
            case CANCELLED -> CompletionType.MID_CANCELLED;
            default -> CompletionType.NORMAL;
        };
    }

    private int resolveHoles(CompletionType completionType) {
        return switch (completionType) {
            case NORMAL -> 18;
            case RAIN_CANCELLED, MID_CANCELLED -> 9;
            case NO_SHOW -> 0;
        };
    }

    private BigDecimal computeFee(CaddieFeepolicy policy, CompletionType completionType,
                                  int holes, boolean isHalfBack, Integer playerCount) {
        BigDecimal fee = switch (completionType) {
            case NORMAL -> isHalfBack && policy.getHalfRoundFee() != null
                    ? policy.getHalfRoundFee()
                    : policy.getFullRoundFee();
            case RAIN_CANCELLED, MID_CANCELLED -> feePolicyService.computeRainFee(policy, holes, isHalfBack);
            case NO_SHOW -> feePolicyService.computeNoShowFee(policy);
        };

        // 5인 플레이 팀은 정상 완료 시에만 추가 캐디피 가산 (2~4인 동일 요금)
        if (completionType == CompletionType.NORMAL
                && playerCount != null && playerCount >= 5
                && policy.getExtraPlayerFee() != null) {
            fee = fee.add(policy.getExtraPlayerFee());
        }
        return fee;
    }
}
