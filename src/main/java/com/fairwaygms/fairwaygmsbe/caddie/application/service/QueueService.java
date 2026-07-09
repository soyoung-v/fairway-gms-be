package com.fairwaygms.fairwaygmsbe.caddie.application.service;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.auth.exception.AuthErrorCode;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.AdjustQueueReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.InitializeQueueReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.InitializeQueueRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.QueueRes;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieGroup;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieQueue;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieQueueHistory;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.QueueRotationState;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieGroupAssignmentType;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieStatus;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.QueueChangeType;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieGroupRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieQueueHistoryRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieQueueRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.QueueRotationStateRepository;
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
import java.util.ArrayList;
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
    private final CaddieGroupRepository caddieGroupRepository;
    private final QueueRotationStateRepository rotationStateRepository;
    private final GolfCourseRepository golfCourseRepository;
    private final UserRepository userRepository;
    private final com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver contextResolver;

    // ADMIN은 X-Selected-Golf-Course-Id 헤더의 선택 골프장, MANAGER는 소속 골프장을 대상으로 한다
    private Long targetGolfCourseId(AuthenticatedUser auth) {
        return auth.isAdmin() ? contextResolver.resolveTargetGolfCourseId(auth) : auth.getGolfCourseId();
    }

    // FR-312: 날짜 기준 대기 순번 목록 조회
    @Transactional(readOnly = true)
    public List<QueueRes> getQueues(Long golfCourseId, LocalDate queueDate, AuthenticatedUser auth) {
        validateManager(auth);
        Long targetId = targetGolfCourseId(auth);
        return queueRepository
                .findByGolfCourse_IdAndQueueDateAndIsDeletedFalseOrderByQueueNumberAsc(targetId, queueDate)
                .stream()
                .map(QueueRes::from)
                .toList();
    }

    // FR-313: 대기 순번 초기화 — 그룹별 순번 이월(CARRY_OVER) 반영
    // 배정 순서: PRIORITY_FIRST 그룹(등록 순) → HOUSE 그룹 → SESSION_FIXED는 제외(수동 배정 전용)
    // 각 그룹은 QueueRotationState.nextStartCaddie 기준으로 정렬 시작점 결정
    // 기존 순번이 있으면 소프트 삭제 후 재생성, 이력(RESET)을 남긴다
    public InitializeQueueRes initializeQueues(InitializeQueueReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = targetGolfCourseId(auth);
        GolfCourse golfCourse = findGolfCourse(golfCourseId);
        User changedBy = findUser(auth.getUserId());

        // 비관적 락으로 기존 순번 행을 점유하여 동시 초기화 방지
        List<CaddieQueue> existing = queueRepository.findForUpdateByGolfCourseAndDate(golfCourseId, req.queueDate());
        Map<Long, Integer> beforeMap = existing.stream()
                .collect(Collectors.toMap(q -> q.getCaddie().getId(), CaddieQueue::getQueueNumber));
        existing.forEach(CaddieQueue::softDelete);

        // 그룹 rotation state 맵 (groupId → nextStartCaddie)
        Map<Long, QueueRotationState> rotationMap = rotationStateRepository.findByGolfCourse_Id(golfCourseId)
                .stream()
                .collect(Collectors.toMap(s -> s.getCaddieGroup().getId(), s -> s));

        // 전체 ACTIVE 캐디를 그룹별로 분류 (null 그룹은 HOUSE로 취급)
        List<Caddie> allActive = caddieRepository.findByGolfCourse_IdAndStatusAndIsDeletedFalse(
                golfCourseId, CaddieStatus.ACTIVE);
        Map<Long, List<Caddie>> byGroup = allActive.stream()
                .filter(c -> c.getCaddieGroup() != null)
                .collect(Collectors.groupingBy(c -> c.getCaddieGroup().getId()));

        // 그룹 없는 캐디(null) — 기본 HOUSE 그룹이 없는 레거시 데이터 포함
        List<Caddie> ungrouped = allActive.stream()
                .filter(c -> c.getCaddieGroup() == null)
                .sorted((a, b) -> compareCaddieNumber(a, b))
                .collect(Collectors.toList());

        // 그룹 목록: PRIORITY_FIRST 우선, SESSION_FIXED는 자동배정 큐에서 제외
        List<CaddieGroup> groups = caddieGroupRepository
                .findByGolfCourse_IdAndIsDeletedFalseOrderByAssignmentTypeAscNameAsc(golfCourseId);

        List<Caddie> orderedCaddies = new ArrayList<>();

        // PRIORITY_FIRST 그룹 먼저 추가
        groups.stream()
                .filter(g -> g.getAssignmentType() == CaddieGroupAssignmentType.PRIORITY_FIRST)
                .forEach(g -> orderedCaddies.addAll(
                        rotationSortedCaddies(byGroup.getOrDefault(g.getId(), List.of()), rotationMap.get(g.getId()))
                ));

        // HOUSE 그룹 추가
        groups.stream()
                .filter(g -> g.getAssignmentType() == CaddieGroupAssignmentType.HOUSE)
                .forEach(g -> orderedCaddies.addAll(
                        rotationSortedCaddies(byGroup.getOrDefault(g.getId(), List.of()), rotationMap.get(g.getId()))
                ));

        // 그룹 미지정 캐디 — 가장 마지막에 추가 (레거시 데이터)
        orderedCaddies.addAll(ungrouped);

        // SESSION_FIXED 그룹 캐디는 큐 번호 없이 자동배정 풀에서 제외
        // 단, 이력은 남겨야 하므로 별도로 SESSION_FIXED 캐디만 queueNumber=0 없이 처리하지 않음
        // → SESSION_FIXED 캐디도 큐 행을 생성하되, autoAssign에서 그룹 타입으로 필터링
        groups.stream()
                .filter(g -> g.getAssignmentType() == CaddieGroupAssignmentType.SESSION_FIXED)
                .forEach(g -> orderedCaddies.addAll(
                        rotationSortedCaddies(byGroup.getOrDefault(g.getId(), List.of()), rotationMap.get(g.getId()))
                ));

        // 순번 부여 및 큐 생성
        IntStream.range(0, orderedCaddies.size()).forEach(i -> {
            Caddie caddie = orderedCaddies.get(i);
            int newNumber = i + 1;
            queueRepository.save(CaddieQueue.create(caddie, golfCourse, req.queueDate(), newNumber));
            queueHistoryRepository.save(CaddieQueueHistory.record(
                    caddie, golfCourse, req.queueDate(),
                    QueueChangeType.RESET,
                    beforeMap.get(caddie.getId()),
                    newNumber, null, changedBy));
        });

        return new InitializeQueueRes(orderedCaddies.size(), req.queueDate());
    }

    // 그룹 캐디 목록을 rotation state 기준 시작점부터 순환 정렬
    private List<Caddie> rotationSortedCaddies(List<Caddie> caddies, QueueRotationState rotationState) {
        if (caddies.isEmpty()) return List.of();

        List<Caddie> sorted = new ArrayList<>(caddies);
        sorted.sort((a, b) -> compareCaddieNumber(a, b));

        if (rotationState == null || rotationState.getNextStartCaddie() == null) {
            return sorted;
        }

        Long startId = rotationState.getNextStartCaddie().getId();
        int startIdx = IntStream.range(0, sorted.size())
                .filter(i -> sorted.get(i).getId().equals(startId))
                .findFirst()
                .orElse(0);

        // startIdx부터 순환: [startIdx..end] + [0..startIdx-1]
        List<Caddie> rotated = new ArrayList<>();
        rotated.addAll(sorted.subList(startIdx, sorted.size()));
        rotated.addAll(sorted.subList(0, startIdx));
        return rotated;
    }

    private int compareCaddieNumber(Caddie a, Caddie b) {
        String na = a.getCaddieNumber() == null ? "" : a.getCaddieNumber();
        String nb = b.getCaddieNumber() == null ? "" : b.getCaddieNumber();
        return na.compareTo(nb);
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
        if (auth.getRole() != UserRole.MANAGER && !auth.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateGolfCourseAccess(Long caddieGolfCourseId, AuthenticatedUser auth) {
        if (auth.isAdmin()) return;
        if (!caddieGolfCourseId.equals(targetGolfCourseId(auth))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
