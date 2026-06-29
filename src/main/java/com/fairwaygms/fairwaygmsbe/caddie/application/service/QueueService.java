package com.fairwaygms.fairwaygmsbe.caddie.application.service;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.auth.exception.AuthErrorCode;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.AdjustQueueReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.InitializeQueueReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.InitializeQueueRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.QueueRes;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieQueue;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieQueueHistory;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieStatus;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.QueueChangeType;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieQueueHistoryRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieQueueRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.caddie.exception.CaddieErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional
public class QueueService {

    private final CaddieRepository caddieRepository;
    private final CaddieQueueRepository queueRepository;
    private final CaddieQueueHistoryRepository queueHistoryRepository;
    private final GolfCourseRepository golfCourseRepository;
    private final UserRepository userRepository;

    // FR-312: 날짜 기준 대기 순번 목록 조회
    @Transactional(readOnly = true)
    public List<QueueRes> getQueues(Long golfCourseId, LocalDate queueDate, AuthenticatedUser auth) {
        validateManager(auth);
        Long targetId = auth.isAdmin() ? golfCourseId : auth.getGolfCourseId();
        return queueRepository
                .findByGolfCourse_IdAndQueueDateAndIsDeletedFalseOrderByQueueNumberAsc(targetId, queueDate)
                .stream()
                .map(QueueRes::from)
                .toList();
    }

    // FR-313: 대기 순번 초기화 — ACTIVE 캐디 전원을 캐디번호 오름차순으로 1번부터 순번 부여
    // 기존 순번이 있으면 소프트 삭제 후 재생성, 이력(RESET)을 남긴다
    public InitializeQueueRes initializeQueues(InitializeQueueReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = auth.getGolfCourseId();
        GolfCourse golfCourse = findGolfCourse(golfCourseId);
        User changedBy = findUser(auth.getUserId());

        // 비관적 락으로 기존 순번 행을 점유하여 동시 초기화 방지
        List<CaddieQueue> existing = queueRepository.findForUpdateByGolfCourseAndDate(golfCourseId, req.queueDate());

        // 기존 순번 맵 (caddieId → beforeNumber)
        Map<Long, Integer> beforeMap = existing.stream()
                .collect(Collectors.toMap(q -> q.getCaddie().getId(), CaddieQueue::getQueueNumber));

        // 기존 순번 소프트 삭제
        existing.forEach(CaddieQueue::softDelete);

        // ACTIVE 캐디 전원을 캐디번호 오름차순으로 신규 순번 부여
        // sort()를 위해 가변 리스트로 복사
        List<Caddie> activeCaddies = new java.util.ArrayList<>(
                caddieRepository.findByGolfCourse_IdAndStatusAndIsDeletedFalse(golfCourseId, CaddieStatus.ACTIVE));
        activeCaddies.sort((a, b) -> {
            String na = a.getCaddieNumber() == null ? "" : a.getCaddieNumber();
            String nb = b.getCaddieNumber() == null ? "" : b.getCaddieNumber();
            return na.compareTo(nb);
        });

        IntStream.range(0, activeCaddies.size()).forEach(i -> {
            Caddie caddie = activeCaddies.get(i);
            int newNumber = i + 1;
            CaddieQueue newQueue = CaddieQueue.create(caddie, golfCourse, req.queueDate(), newNumber);
            queueRepository.save(newQueue);

            // 초기화 이력 저장
            CaddieQueueHistory history = CaddieQueueHistory.record(
                    caddie, golfCourse, req.queueDate(),
                    QueueChangeType.RESET,
                    beforeMap.get(caddie.getId()), // 이전 순번 (null이면 새로 등록)
                    newNumber,
                    null,
                    changedBy
            );
            queueHistoryRepository.save(history);
        });

        return new InitializeQueueRes(activeCaddies.size(), req.queueDate());
    }

    // FR-314: 순번 수동 조정 — 사유 필수, 비관적 락으로 중복 순번 방지
    public QueueRes adjustQueue(Long caddieId, AdjustQueueReq req, AuthenticatedUser auth) {
        validateManager(auth);

        // 수동 조정 시 사유 필수 (FR-314)
        if (req.reason() == null || req.reason().isBlank()) {
            throw new BusinessException(CaddieErrorCode.QUEUE_ADJUST_REASON_REQUIRED);
        }

        Caddie caddie = caddieRepository.findByIdAndIsDeletedFalse(caddieId)
                .orElseThrow(() -> new BusinessException(CaddieErrorCode.CADDIE_NOT_FOUND));
        validateGolfCourseAccess(caddie.getGolfCourse().getId(), auth);

        Long golfCourseId = caddie.getGolfCourse().getId();
        GolfCourse golfCourse = findGolfCourse(golfCourseId);
        User changedBy = findUser(auth.getUserId());

        // 같은 날짜 전체 순번을 비관적 락으로 점유 — 조정 중 다른 트랜잭션의 중복 순번 방지
        queueRepository.findForUpdateByGolfCourseAndDate(golfCourseId, req.queueDate());

        // 대상 캐디의 현재 순번 조회
        CaddieQueue queue = queueRepository
                .findByCaddie_IdAndQueueDateAndIsDeletedFalse(caddieId, req.queueDate())
                .orElseThrow(() -> new BusinessException(CaddieErrorCode.QUEUE_NOT_FOUND));

        // 목표 순번이 이미 사용 중인지 확인
        if (queueRepository.existsByGolfCourse_IdAndQueueDateAndQueueNumberAndIsDeletedFalse(
                golfCourseId, req.queueDate(), req.queueNumber())) {
            throw new BusinessException(CaddieErrorCode.DUPLICATE_QUEUE_NUMBER);
        }

        int beforeNumber = queue.getQueueNumber();
        queue.adjustNumber(req.queueNumber());

        // 수동 조정 이력 저장
        CaddieQueueHistory history = CaddieQueueHistory.record(
                caddie, golfCourse, req.queueDate(),
                QueueChangeType.MANUAL_ADJUST,
                beforeNumber,
                req.queueNumber(),
                req.reason(),
                changedBy
        );
        queueHistoryRepository.save(history);

        return QueueRes.from(queue);
    }

    private GolfCourse findGolfCourse(Long golfCourseId) {
        return golfCourseRepository.findByIdAndIsDeletedFalse(golfCourseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOLF_COURSE_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    }

    private void validateManager(AuthenticatedUser auth) {
        if (auth.getRole() != UserRole.MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateGolfCourseAccess(Long caddieGolfCourseId, AuthenticatedUser auth) {
        if (auth.isAdmin()) return;
        if (!caddieGolfCourseId.equals(auth.getGolfCourseId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
