package com.fairwaygms.fairwaygmsbe.settlement.application.service;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.settlement.application.model.req.AdjustCaddieFeeReq;
import com.fairwaygms.fairwaygmsbe.settlement.application.model.res.*;
import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.MonthlySettlement;
import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.MonthlySettlementCaddie;
import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.SettlementChangeHistory;
import com.fairwaygms.fairwaygmsbe.settlement.domain.enums.SettlementChangeType;
import com.fairwaygms.fairwaygmsbe.settlement.domain.repository.*;
import com.fairwaygms.fairwaygmsbe.settlement.exception.SettlementErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonthlySettlementService {

    private final MonthlySettlementRepository settlementRepository;
    private final MonthlySettlementCaddieRepository settlementCaddieRepository;
    private final AssignmentRecordRepository assignmentRecordRepository;
    private final SettlementChangeHistoryRepository historyRepository;
    private final CaddieRepository caddieRepository;
    // ADMIN은 golfCourseId claim이 null이므로 선택 골프장 헤더 기준으로 대상을 결정한다 (FR-610/613)
    private final com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver contextResolver;

    // 정산 조회/집계/확정은 MANAGER·ADMIN 전용 — CADDY 접근 차단
    private void requireManagerOrAdmin(AuthenticatedUser auth) {
        if (!auth.isManager() && !auth.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    @Transactional(readOnly = true)
    public List<RoundSummaryRes> getRoundSummary(String yearMonth, AuthenticatedUser auth) {
        requireManagerOrAdmin(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);
        List<Object[]> rows = assignmentRecordRepository.aggregateByCaddie(golfCourseId, yearMonth);
        Map<Long, String> nameMap = buildCaddieNameMap(golfCourseId);

        return rows.stream()
                .map(row -> new RoundSummaryRes(
                        (Long) row[0],
                        nameMap.getOrDefault((Long) row[0], "알 수 없음"),
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IncomeSummaryRes> getIncomeSummary(String yearMonth, AuthenticatedUser auth) {
        requireManagerOrAdmin(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);
        List<Object[]> rows = assignmentRecordRepository.aggregateByCaddie(golfCourseId, yearMonth);
        Map<Long, String> nameMap = buildCaddieNameMap(golfCourseId);

        // 마감 확정이 있으면 adjustedFee를 반영한다
        Map<Long, BigDecimal> adjustedMap = settlementRepository
                .findByGolfCourseIdAndSettlementYearMonthAndIsDeletedFalse(golfCourseId, yearMonth)
                .map(s -> settlementCaddieRepository.findByMonthlySettlementId(s.getId())
                        .stream()
                        .collect(Collectors.toMap(
                                MonthlySettlementCaddie::getCaddieId,
                                MonthlySettlementCaddie::getAdjustedFee)))
                .orElse(Map.of());

        return rows.stream()
                .map(row -> {
                    Long caddieId = (Long) row[0];
                    BigDecimal totalFee = (BigDecimal) row[3];
                    BigDecimal adjustedFee = adjustedMap.getOrDefault(caddieId, totalFee);
                    return new IncomeSummaryRes(
                            caddieId,
                            nameMap.getOrDefault(caddieId, "알 수 없음"),
                            totalFee,
                            adjustedFee);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public MonthlySettlementRes confirmMonth(String yearMonth, AuthenticatedUser auth) {
        requireManagerOrAdmin(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);
        MonthlySettlement settlement = getOrCreateSettlement(golfCourseId, yearMonth);

        if (settlement.isConfirmed()) {
            throw new BusinessException(SettlementErrorCode.SETTLEMENT_ALREADY_CONFIRMED);
        }

        // assignment_record를 집계하여 monthly_settlement_caddie 생성/갱신
        List<Object[]> rows = assignmentRecordRepository.aggregateByCaddie(golfCourseId, yearMonth);
        for (Object[] row : rows) {
            Long caddieId = (Long) row[0];
            int rounds = ((Number) row[1]).intValue();
            int assignments = ((Number) row[2]).intValue();
            BigDecimal totalFee = (BigDecimal) row[3];

            MonthlySettlementCaddie caddie = settlementCaddieRepository
                    .findByMonthlySettlementIdAndCaddieId(settlement.getId(), caddieId)
                    .orElse(null);

            if (caddie == null) {
                settlementCaddieRepository.save(
                        MonthlySettlementCaddie.create(settlement.getId(), caddieId,
                                golfCourseId, rounds, assignments, totalFee));
            } else {
                caddie.aggregate(0, 0, BigDecimal.ZERO); // 이미 집계된 경우 무시
            }

            // assignment_record 확정 처리
            assignmentRecordRepository
                    .findByGolfCourseIdAndSettlementYearMonthAndIsDeletedFalse(golfCourseId, yearMonth)
                    .forEach(r -> {
                        if (!r.getIsConfirmed()) r.confirm();
                    });
        }

        settlement.confirm();
        return MonthlySettlementRes.from(settlement);
    }

    @Transactional
    public void cancelConfirmMonth(String yearMonth, AuthenticatedUser auth) {
        requireManagerOrAdmin(auth);
        MonthlySettlement settlement = settlementRepository
                .findByGolfCourseIdAndSettlementYearMonthAndIsDeletedFalse(
                        contextResolver.resolveTargetGolfCourseId(auth), yearMonth)
                .orElseThrow(() -> new BusinessException(SettlementErrorCode.SETTLEMENT_NOT_FOUND));

        if (!settlement.isConfirmed()) {
            throw new BusinessException(SettlementErrorCode.SETTLEMENT_NOT_CONFIRMED);
        }

        settlement.cancelConfirm();
    }

    @Transactional
    public IncomeSummaryRes adjustCaddieFee(Long caddieId, AdjustCaddieFeeReq req, AuthenticatedUser auth) {
        requireManagerOrAdmin(auth);
        Long golfCourseId = contextResolver.resolveTargetGolfCourseId(auth);
        MonthlySettlement settlement = getOrCreateSettlement(golfCourseId, req.yearMonth());

        if (settlement.isConfirmed()) {
            throw new BusinessException(SettlementErrorCode.SETTLEMENT_ALREADY_CONFIRMED);
        }

        MonthlySettlementCaddie caddie = settlementCaddieRepository
                .findByMonthlySettlementIdAndCaddieId(settlement.getId(), caddieId)
                .orElseGet(() -> {
                    // 아직 집계 전이면 assignment_record에서 합산하여 캐디 행 생성
                    BigDecimal total = assignmentRecordRepository
                            .findByGolfCourseIdAndSettlementYearMonthAndIsDeletedFalse(
                                    golfCourseId, req.yearMonth())
                            .stream()
                            .filter(r -> r.getCaddieId().equals(caddieId) && r.getFeeAmount() != null)
                            .map(r -> r.getFeeAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return settlementCaddieRepository.save(
                            MonthlySettlementCaddie.create(settlement.getId(), caddieId,
                                    golfCourseId, 0, 0, total));
                });

        BigDecimal before = caddie.getAdjustedFee();
        caddie.adjustFee(req.adjustedFee(), req.reason());

        historyRepository.save(SettlementChangeHistory.create(
                caddie.getId(), golfCourseId,
                SettlementChangeType.MANUAL_ADJUST,
                before, req.adjustedFee(), req.reason(), auth.getUserId()));

        String caddieName = caddieRepository.findById(caddieId)
                .map(Caddie::getName).orElse("알 수 없음");

        return new IncomeSummaryRes(caddieId, caddieName, caddie.getTotalFee(), caddie.getAdjustedFee());
    }

    @Transactional(readOnly = true)
    public Page<SettlementHistoryRes> getHistory(String yearMonth, Long caddieId,
                                                 int page, int size, AuthenticatedUser auth) {
        requireManagerOrAdmin(auth);
        return historyRepository.findByYearMonthAndOptionalCaddie(
                        contextResolver.resolveTargetGolfCourseId(auth), yearMonth, caddieId, PageRequest.of(page, size))
                .map(SettlementHistoryRes::from);
    }

    // ─── 내부 헬퍼 ──────────────────────────────────────────────────────────────

    private MonthlySettlement getOrCreateSettlement(Long golfCourseId, String yearMonth) {
        return settlementRepository
                .findByGolfCourseIdAndSettlementYearMonthAndIsDeletedFalse(golfCourseId, yearMonth)
                .orElseGet(() -> settlementRepository.save(
                        MonthlySettlement.create(golfCourseId, yearMonth)));
    }

    private Map<Long, String> buildCaddieNameMap(Long golfCourseId) {
        return caddieRepository.findByGolfCourse_IdAndIsDeletedFalse(golfCourseId).stream()
                .collect(Collectors.toMap(Caddie::getId, Caddie::getName));
    }
}
