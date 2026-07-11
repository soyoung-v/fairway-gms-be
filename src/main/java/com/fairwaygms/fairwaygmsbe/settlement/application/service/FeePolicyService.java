package com.fairwaygms.fairwaygmsbe.settlement.application.service;

import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentRepository;
import com.fairwaygms.fairwaygmsbe.assignment.exception.AssignmentErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.settlement.application.model.req.NoShowCalculateReq;
import com.fairwaygms.fairwaygmsbe.settlement.application.model.req.RainCancellationCalculateReq;
import com.fairwaygms.fairwaygmsbe.settlement.application.model.req.UpsertFeePolicyReq;
import com.fairwaygms.fairwaygmsbe.settlement.application.model.res.FeeCalculationRes;
import com.fairwaygms.fairwaygmsbe.settlement.application.model.res.FeePolicyRes;
import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.CaddieFeepolicy;
import com.fairwaygms.fairwaygmsbe.settlement.domain.enums.NoShowPolicy;
import com.fairwaygms.fairwaygmsbe.settlement.domain.repository.CaddieFeepolicyRepository;
import com.fairwaygms.fairwaygmsbe.settlement.exception.SettlementErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class FeePolicyService {

    private static final int FULL_ROUND_HOLES = 18;

    private final CaddieFeepolicyRepository feePolicyRepository;
    private final AssignmentRepository assignmentRepository;
    private final com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver contextResolver;

    // ADMIN은 X-Selected-Golf-Course-Id 헤더의 선택 골프장, MANAGER는 소속 골프장을 대상으로 한다
    private Long targetGolfCourseId(AuthenticatedUser auth) {
        return auth.isAdmin() ? contextResolver.resolveTargetGolfCourseId(auth) : auth.getGolfCourseId();
    }

    // 정산/캐디피는 MANAGER·ADMIN 전용 — CADDY 접근 차단
    private void requireManagerOrAdmin(AuthenticatedUser auth) {
        if (!auth.isManager() && !auth.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    @Transactional
    public FeePolicyRes upsertFeePolicy(UpsertFeePolicyReq req, AuthenticatedUser auth) {
        requireManagerOrAdmin(auth);
        Long golfCourseId = targetGolfCourseId(auth);
        CaddieFeepolicy policy = feePolicyRepository.findByGolfCourseIdAndIsDeletedFalse(golfCourseId)
                .orElse(null);

        if (policy == null) {
            policy = CaddieFeepolicy.create(golfCourseId,
                    req.fullRoundFee(), req.halfRoundFee(), req.halfBackType(),
                    req.noShowPolicy(), req.noShowFee(), req.extraPlayerFee());
            policy = feePolicyRepository.save(policy);
        } else {
            policy.update(req.fullRoundFee(), req.halfRoundFee(), req.halfBackType(),
                    req.noShowPolicy(), req.noShowFee(), req.extraPlayerFee());
        }
        return FeePolicyRes.from(policy);
    }

    @Transactional(readOnly = true)
    public FeePolicyRes getFeePolicy(AuthenticatedUser auth) {
        requireManagerOrAdmin(auth);
        return FeePolicyRes.from(getPolicy(targetGolfCourseId(auth)));
    }

    @Transactional(readOnly = true)
    public FeeCalculationRes calculateRainCancellationFee(RainCancellationCalculateReq req,
                                                          AuthenticatedUser auth) {
        requireManagerOrAdmin(auth);
        CaddieFeepolicy policy = getPolicy(targetGolfCourseId(auth));
        Assignment assignment = getAssignment(req.assignmentId());

        BigDecimal fee = computeRainFee(policy, req.playedHoleCount(), assignment.getIsHalfBack());
        return FeeCalculationRes.rainCancellation(fee, req.playedHoleCount());
    }

    @Transactional(readOnly = true)
    public FeeCalculationRes calculateNoShowFee(NoShowCalculateReq req, AuthenticatedUser auth) {
        requireManagerOrAdmin(auth);
        CaddieFeepolicy policy = getPolicy(targetGolfCourseId(auth));
        getAssignment(req.assignmentId()); // 존재 확인

        BigDecimal fee = computeNoShowFee(policy);
        return FeeCalculationRes.noShow(fee, policy.getNoShowPolicy().name());
    }

    // 내부 계산 메서드: 이벤트 리스너에서도 사용 가능
    public BigDecimal computeRainFee(CaddieFeepolicy policy, int playedHoleCount, boolean isHalfBack) {
        if (playedHoleCount <= 0) return BigDecimal.ZERO;

        BigDecimal baseFee = isHalfBack && policy.getHalfRoundFee() != null
                ? policy.getHalfRoundFee()
                : policy.getFullRoundFee();

        // 전체 라운드 기준 비율 계산
        int baseHoles = isHalfBack ? FULL_ROUND_HOLES / 2 : FULL_ROUND_HOLES;
        if (playedHoleCount >= baseHoles) return baseFee;

        return baseFee
                .multiply(BigDecimal.valueOf(playedHoleCount))
                .divide(BigDecimal.valueOf(baseHoles), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal computeNoShowFee(CaddieFeepolicy policy) {
        NoShowPolicy noShowPolicy = policy.getNoShowPolicy();
        return switch (noShowPolicy) {
            case NONE -> BigDecimal.ZERO;
            case HALF -> {
                BigDecimal half = policy.getHalfRoundFee() != null
                        ? policy.getHalfRoundFee()
                        : policy.getFullRoundFee().divide(BigDecimal.TWO, 2, RoundingMode.HALF_UP);
                yield policy.getNoShowFee() != null ? policy.getNoShowFee() : half;
            }
            case FULL -> policy.getNoShowFee() != null
                    ? policy.getNoShowFee()
                    : policy.getFullRoundFee();
        };
    }

    // ─── 내부 헬퍼 ──────────────────────────────────────────────────────────────

    public CaddieFeepolicy getPolicy(Long golfCourseId) {
        return feePolicyRepository.findByGolfCourseIdAndIsDeletedFalse(golfCourseId)
                .orElseThrow(() -> new BusinessException(SettlementErrorCode.FEE_POLICY_NOT_FOUND));
    }

    private Assignment getAssignment(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND));
    }
}
