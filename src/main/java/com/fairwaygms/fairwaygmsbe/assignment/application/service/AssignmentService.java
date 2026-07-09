package com.fairwaygms.fairwaygmsbe.assignment.application.service;

import com.fairwaygms.fairwaygmsbe.assignment.application.model.req.*;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.AssignmentHistoryRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.AssignmentRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.AutoAssignRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.CourseAssignmentRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.HalfBackAssignRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.RainCancellationRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.ValidationErrorRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.UnassignedTeamRes;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.AssignmentHistory;
import com.fairwaygms.fairwaygmsbe.assignment.domain.enums.AssignmentChangeType;
import com.fairwaygms.fairwaygmsbe.assignment.domain.enums.AssignmentStatus;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentHistoryRepository;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentRepository;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.CartAssignmentRepository;
import com.fairwaygms.fairwaygmsbe.assignment.exception.AssignmentErrorCode;
import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.auth.exception.AuthErrorCode;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieGroup;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieQueue;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.QueueRotationState;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieGroupAssignmentType;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.DailyStatusType;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.QueueChangeType;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieDailyStatusRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieGroupRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieQueueHistoryRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieQueueRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.QueueRotationStateRepository;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieQueueHistory;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationPeriod;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.RainCancellationPolicy;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.RainCancellationPolicyType;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.RainCancellationPolicyRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.ReservationTeam;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.TeeTime;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.OperationPeriodRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.OperationSettingRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.ReservationTeamRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.TeeTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentService {

    // 운영 설정이 없을 때의 기본 일일 최대 배정 수 — 실제 한도는 그날 운영 부 수를 따름 (3부제면 3근무)
    private static final int DEFAULT_MAX_DAILY_ASSIGNMENTS = 2;
    private static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    // 배정 제외 상태 — 이 상태의 캐디는 자동배정 풀에서 제외 (큐 순번은 보존)
    private static final Set<DailyStatusType> EXCLUDED_STATUSES = Set.of(
            DailyStatusType.DAY_OFF, DailyStatusType.ABSENCE, DailyStatusType.ASSIGN_EXCLUDED
    );

    private final AssignmentRepository assignmentRepository;
    private final AssignmentHistoryRepository historyRepository;
    private final CartAssignmentRepository cartAssignmentRepository;
    private final ReservationTeamRepository reservationTeamRepository;
    private final CaddieRepository caddieRepository;
    private final CaddieQueueRepository queueRepository;
    private final CaddieQueueHistoryRepository queueHistoryRepository;
    private final CaddieGroupRepository caddieGroupRepository;
    private final QueueRotationStateRepository rotationStateRepository;
    private final CaddieDailyStatusRepository caddieDailyStatusRepository;
    private final TeeTimeRepository teeTimeRepository;
    private final RainCancellationPolicyRepository rainCancellationPolicyRepository;
    private final OperationSettingRepository operationSettingRepository;
    private final OperationPeriodRepository operationPeriodRepository;
    private final GolfCourseRepository golfCourseRepository;
    private final UserRepository userRepository;
    private final com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver contextResolver;

    // ADMIN은 X-Selected-Golf-Course-Id 헤더의 선택 골프장, MANAGER는 소속 골프장을 대상으로 한다
    private Long targetGolfCourseId(AuthenticatedUser auth) {
        return auth.isAdmin() ? contextResolver.resolveTargetGolfCourseId(auth) : auth.getGolfCourseId();
    }

    // 배정 변경 이력 조회 — 날짜 + 캐디(선택) 필터 (FR-524)
    @Transactional(readOnly = true)
    public List<AssignmentHistoryRes> getHistory(Long golfCourseId, LocalDate assignmentDate,
                                                  Long caddieId, AuthenticatedUser auth) {
        // FR-524: Admin+Manager 조회 가능 — ADMIN은 X-Selected-Golf-Course-Id 헤더 기준
        if (!auth.isAdmin()) {
            validateManager(auth);
        }
        Long targetId = contextResolver.resolveTargetGolfCourseId(auth);
        List<com.fairwaygms.fairwaygmsbe.assignment.domain.entity.AssignmentHistory> histories =
                caddieId != null
                        ? historyRepository.findByGolfCourseAndDateAndCaddie(targetId, assignmentDate, caddieId)
                        : historyRepository.findByGolfCourse_IdAndAssignment_AssignmentDateOrderByCreatedAtAsc(targetId, assignmentDate);
        return histories.stream().map(AssignmentHistoryRes::from).toList();
    }

    // 배정 목록 조회 — 골프장+날짜 기준 (티타임 순 정렬)
    @Transactional(readOnly = true)
    public List<AssignmentRes> getAssignments(Long golfCourseId, LocalDate date, AuthenticatedUser auth) {
        validateManager(auth);
        Long targetId = targetGolfCourseId(auth);
        return assignmentRepository.findByGolfCourseAndDateWithDetails(targetId, date)
                .stream()
                .map(AssignmentRes::from)
                .toList();
    }

    // 코스별 배정표 조회 — courseId 기준 필터 + 카트 정보 포함 (API-512, FR-519)
    @Transactional(readOnly = true)
    public List<CourseAssignmentRes> getAssignmentsByCourse(Long golfCourseId, LocalDate date,
                                                             Long courseId, AuthenticatedUser auth) {
        validateManager(auth);
        Long targetId = targetGolfCourseId(auth);

        List<com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment> assignments =
                assignmentRepository.findByGolfCourseAndDateAndCourse(targetId, date, courseId);

        // teeTimeId → CartAssignment 맵 구성
        Map<Long, com.fairwaygms.fairwaygmsbe.assignment.domain.entity.CartAssignment> cartMap =
                cartAssignmentRepository.findActiveByGolfCourseAndDate(targetId, date)
                        .stream()
                        .collect(Collectors.toMap(
                                ca -> ca.getTeeTime().getId(),
                                ca -> ca,
                                (a, b) -> a));

        return assignments.stream()
                .map(a -> CourseAssignmentRes.from(
                        a,
                        cartMap.get(a.getReservationTeam().getTeeTime().getId())))
                .toList();
    }

    // 수동 사전 배정 — 특정 예약팀에 특정 캐디를 직접 배정
    // isLocked=true이면 자동배정 풀에서 제외되고 Manager 권한으로만 해제 가능 (FR-512)
    public AssignmentRes manualPreAssign(ManualPreAssignReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = targetGolfCourseId(auth);
        GolfCourse golfCourse = findGolfCourse(golfCourseId);
        User manager = findUser(auth.getUserId());

        ReservationTeam team = reservationTeamRepository.findByIdAndIsDeletedFalse(req.reservationTeamId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        validateGolfCourseAccess(team.getGolfCourse().getId(), auth);

        if (assignmentRepository.existsByReservationTeam_IdAndIsDeletedFalse(req.reservationTeamId())) {
            throw new BusinessException(AssignmentErrorCode.ASSIGNMENT_ALREADY_EXISTS);
        }

        Caddie caddie = caddieRepository.findByIdAndIsDeletedFalse(req.caddieId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        validateGolfCourseAccess(caddie.getGolfCourse().getId(), auth);

        LocalDate assignmentDate = team.getTeeTime().getPlayDate();
        validateDailyLimit(golfCourseId, caddie.getId(), assignmentDate, req.isHalfBack());

        Assignment assignment = Assignment.create(golfCourse, team, caddie, assignmentDate,
                req.isLocked(), req.isHalfBack());
        assignmentRepository.save(assignment);

        historyRepository.save(AssignmentHistory.record(
                assignment, golfCourse, AssignmentChangeType.MANUAL,
                null, caddie, req.reason(), manager));

        return AssignmentRes.from(assignment);
    }

    // 하프백(투근무) 배정 — 캐디 1명에게 같은 날 두 팀을 한 번에 배정 (FR-506)
    public HalfBackAssignRes halfBackAssign(HalfBackAssignReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = targetGolfCourseId(auth);
        GolfCourse golfCourse = findGolfCourse(golfCourseId);
        User manager = findUser(auth.getUserId());

        if (req.reservationTeamId1().equals(req.reservationTeamId2())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        Caddie caddie = caddieRepository.findByIdAndIsDeletedFalse(req.caddieId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        validateGolfCourseAccess(caddie.getGolfCourse().getId(), auth);

        ReservationTeam team1 = findTeamWithAccess(req.reservationTeamId1(), auth);
        ReservationTeam team2 = findTeamWithAccess(req.reservationTeamId2(), auth);

        // 투근무는 같은 날 두 팀 담당이 전제
        LocalDate assignmentDate = team1.getTeeTime().getPlayDate();
        if (!assignmentDate.equals(team2.getTeeTime().getPlayDate())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        for (Long teamId : List.of(req.reservationTeamId1(), req.reservationTeamId2())) {
            if (assignmentRepository.existsByReservationTeam_IdAndIsDeletedFalse(teamId)) {
                throw new BusinessException(AssignmentErrorCode.ASSIGNMENT_ALREADY_EXISTS);
            }
        }

        // 기존 배정 포함 2건 추가가 그날 운영 부 수 한도를 넘으면 거절
        int currentCount = assignmentRepository
                .countByCaddie_IdAndAssignmentDateAndIsDeletedFalse(caddie.getId(), assignmentDate);
        if (currentCount + 2 > resolveDailyMaxAssignments(golfCourseId, assignmentDate)) {
            throw new BusinessException(AssignmentErrorCode.CADDIE_ASSIGNMENT_LIMIT_EXCEEDED);
        }

        Assignment a1 = assignmentRepository.save(
                Assignment.create(golfCourse, team1, caddie, assignmentDate, false, true));
        Assignment a2 = assignmentRepository.save(
                Assignment.create(golfCourse, team2, caddie, assignmentDate, false, true));
        historyRepository.save(AssignmentHistory.record(
                a1, golfCourse, AssignmentChangeType.MANUAL, null, caddie, req.reason(), manager));
        historyRepository.save(AssignmentHistory.record(
                a2, golfCourse, AssignmentChangeType.MANUAL, null, caddie, req.reason(), manager));

        return new HalfBackAssignRes(a1.getId(), a2.getId(), true);
    }

    private ReservationTeam findTeamWithAccess(Long teamId, AuthenticatedUser auth) {
        ReservationTeam team = reservationTeamRepository.findByIdAndIsDeletedFalse(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        validateGolfCourseAccess(team.getGolfCourse().getId(), auth);
        return team;
    }

    // SESSION_FIXED 그룹 일괄 수동 배정 — 지정 티타임부터 그룹 캐디를 순서대로 배정
    // 부반(주중2부반 등)이 특정 부의 첫 팀부터 일괄 배정될 때 사용
    public List<AssignmentRes> bulkSessionAssign(BulkSessionAssignReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = targetGolfCourseId(auth);
        GolfCourse golfCourse = findGolfCourse(golfCourseId);
        User manager = findUser(auth.getUserId());

        TeeTime startTeeTime = teeTimeRepository.findByIdAndIsDeletedFalse(req.startTeeTimeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        validateGolfCourseAccess(startTeeTime.getGolfCourse().getId(), auth);

        // 같은 부+날짜의 모든 예약팀 (startTime 오름차순), 시작 티타임 이후만 대상
        Long periodId = startTeeTime.getOperationPeriod().getId();
        List<ReservationTeam> allTeams = reservationTeamRepository.findByPeriodIdAndPlayDate(periodId, req.assignmentDate());

        // 기존 활성 배정이 없는 팀만 필터, 시작 티타임 이후 팀만 포함
        Set<Long> alreadyAssigned = getAlreadyAssignedTeamIds(golfCourseId, req.assignmentDate());
        List<ReservationTeam> targetTeams = allTeams.stream()
                .filter(t -> !alreadyAssigned.contains(t.getId()))
                .filter(t -> !t.getTeeTime().getStartTime().isBefore(startTeeTime.getStartTime()))
                .collect(Collectors.toList());

        if (targetTeams.isEmpty()) return List.of();

        // 그룹 캐디를 큐 순번 기준으로 정렬 (큐가 없으면 caddieNumber 순)
        List<Caddie> groupCaddies = buildGroupCaddiePool(req.caddieGroupId(), req.assignmentDate(), golfCourseId);
        if (groupCaddies.isEmpty()) return List.of();

        // 배정 제외 상태 캐디 집합
        Set<Long> excludedIds = getExcludedCaddieIds(golfCourseId, req.assignmentDate());

        // 배정 카운트 맵 (당일 이미 배정된 건 수)
        Map<Long, Integer> dayCount = buildCaddieDayCountMap(golfCourseId, req.assignmentDate());

        List<AssignmentRes> results = new ArrayList<>();
        int caddiePointer = 0;

        for (ReservationTeam team : targetTeams) {
            // 배정 가능한 다음 캐디 탐색
            Caddie caddie = null;
            for (int i = 0; i < groupCaddies.size(); i++) {
                int idx = (caddiePointer + i) % groupCaddies.size();
                Caddie candidate = groupCaddies.get(idx);
                if (!excludedIds.contains(candidate.getId())
                        && dayCount.getOrDefault(candidate.getId(), 0) < 1) {
                    caddie = candidate;
                    caddiePointer = (idx + 1) % groupCaddies.size();
                    break;
                }
            }
            if (caddie == null) break; // 그룹 캐디 소진 — 남은 팀은 이후 자동배정으로 처리

            Assignment assignment = Assignment.create(golfCourse, team, caddie, req.assignmentDate(), false, false);
            assignmentRepository.save(assignment);
            historyRepository.save(AssignmentHistory.record(
                    assignment, golfCourse, AssignmentChangeType.MANUAL, null, caddie, null, manager));
            dayCount.merge(caddie.getId(), 1, Integer::sum);
            results.add(AssignmentRes.from(assignment));
        }

        return results;
    }

    // 부(部) 단위 자동배정 — queueNumber 순, 그룹 우선순위 적용, 투근무 자동 처리
    // ADR-005 Decision 3: 부별 단위로 실행, 이전 부의 rotation 이어서 사용
    public AutoAssignRes autoAssign(AutoAssignReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = targetGolfCourseId(auth);
        GolfCourse golfCourse = findGolfCourse(golfCourseId);
        User manager = findUser(auth.getUserId());

        // 같은 날짜 배정 행 비관적 락 — 동시 자동배정 방지
        List<Assignment> existingAssignments = assignmentRepository.findForUpdateByGolfCourseAndDate(
                golfCourseId, req.assignmentDate());

        Set<Long> alreadyAssignedTeamIds = existingAssignments.stream()
                .map(a -> a.getReservationTeam().getId())
                .collect(Collectors.toSet());

        Map<Long, Integer> caddieDayCount = existingAssignments.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getCaddie().getId(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        // 큐 비관적 락 — 순번 경합 방지
        List<CaddieQueue> lockedQueue = queueRepository.findForUpdateByGolfCourseAndDate(
                golfCourseId, req.assignmentDate());
        if (lockedQueue.isEmpty()) {
            throw new BusinessException(AssignmentErrorCode.CADDIE_QUEUE_EMPTY);
        }

        // 배정 제외 캐디
        Set<Long> excludedIds = getExcludedCaddieIds(golfCourseId, req.assignmentDate());

        // 배정 대상 캐디 풀 구성 — SESSION_FIXED 제외, 요청 그룹 필터
        Set<Long> requestedGroupIds = (req.groupIds() != null && !req.groupIds().isEmpty())
                ? new HashSet<>(req.groupIds()) : null;

        List<CaddieQueue> pool = lockedQueue.stream()
                .filter(q -> !excludedIds.contains(q.getCaddie().getId()))
                .filter(q -> {
                    CaddieGroup group = q.getCaddie().getCaddieGroup();
                    if (group != null && group.getAssignmentType() == CaddieGroupAssignmentType.SESSION_FIXED) {
                        return false; // SESSION_FIXED는 autoAssign 풀 제외
                    }
                    if (requestedGroupIds != null) {
                        Long groupId = group != null ? group.getId() : null;
                        return requestedGroupIds.contains(groupId);
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if (pool.isEmpty()) {
            throw new BusinessException(AssignmentErrorCode.CADDIE_QUEUE_EMPTY);
        }

        // 배정 대상 예약팀 — 해당 부의 미배정 팀
        List<ReservationTeam> targetTeams = reservationTeamRepository
                .findByPeriodIdAndPlayDate(req.periodId(), req.assignmentDate())
                .stream()
                .filter(t -> !alreadyAssignedTeamIds.contains(t.getId()))
                .collect(Collectors.toList());

        if (targetTeams.isEmpty()) {
            return new AutoAssignRes(req.assignmentDate(), req.periodId(), 0, 0);
        }

        // 그룹별 마지막 배정 캐디 추적 — rotation state 업데이트에 사용
        Map<Long, Caddie> lastAssignedByGroup = new HashMap<>();

        int pointer = 0;
        int poolSize = pool.size();
        int assignedCount = 0;
        int skippedCount = 0;

        // 그날 운영 부 수만큼 다근무 허용 (2부제 투근무, 3부제 3근무)
        int dailyMax = resolveDailyMaxAssignments(golfCourseId, req.assignmentDate());

        for (ReservationTeam team : targetTeams) {
            // 다음 배정 가능한 캐디 탐색 (최대 dailyMax*pool 순회 = 다근무 허용)
            Caddie assigned = null;
            for (int tries = 0; tries < poolSize * dailyMax; tries++) {
                int idx = pointer % poolSize;
                Caddie candidate = pool.get(idx).getCaddie();
                int count = caddieDayCount.getOrDefault(candidate.getId(), 0);
                pointer++;
                if (count < dailyMax) {
                    assigned = candidate;
                    break;
                }
            }

            if (assigned == null) {
                skippedCount++;
                continue;
            }

            int currentCount = caddieDayCount.getOrDefault(assigned.getId(), 0);
            boolean isHalfBack = currentCount >= 1; // 두 번째 이후 배정 = 투근무/3근무

            Assignment assignment = Assignment.create(golfCourse, team, assigned, req.assignmentDate(), false, isHalfBack);
            assignmentRepository.save(assignment);
            historyRepository.save(AssignmentHistory.record(
                    assignment, golfCourse, AssignmentChangeType.AUTO, null, assigned, null, manager));

            caddieDayCount.merge(assigned.getId(), 1, Integer::sum);
            assignedCount++;

            // 그룹별 마지막 배정 캐디 갱신 (SWAP과 무관한 원래 캐디 ID 기준)
            Long groupId = assigned.getCaddieGroup() != null ? assigned.getCaddieGroup().getId() : null;
            if (groupId != null) {
                lastAssignedByGroup.put(groupId, assigned);
            }
        }

        // 그룹별 QueueRotationState 업데이트 — 다음 날 또는 다음 부 시작 캐디 기록
        updateRotationStates(golfCourse, lastAssignedByGroup);

        return new AutoAssignRes(req.assignmentDate(), req.periodId(), assignedCount, skippedCount);
    }

    // 재배정 — 기존 배정 캐디를 새 캐디로 교체 (잠금 자동 해제)
    public AssignmentRes reassign(Long assignmentId, ReassignReq req, AuthenticatedUser auth) {
        validateManager(auth);
        User manager = findUser(auth.getUserId());

        Assignment assignment = findAssignment(assignmentId);
        validateGolfCourseAccess(assignment.getGolfCourse().getId(), auth);
        validateNotCompleted(assignment);

        Caddie beforeCaddie = assignment.getCaddie();
        Caddie newCaddie = caddieRepository.findByIdAndIsDeletedFalse(req.newCaddieId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        validateGolfCourseAccess(newCaddie.getGolfCourse().getId(), auth);

        // 재배정 시 새 캐디의 당일 배정 가능 여부 확인
        validateDailyLimit(assignment.getGolfCourse().getId(), newCaddie.getId(),
                assignment.getAssignmentDate(), assignment.getIsHalfBack());

        assignment.reassign(newCaddie);
        historyRepository.save(AssignmentHistory.record(
                assignment, assignment.getGolfCourse(), AssignmentChangeType.REASSIGN,
                beforeCaddie, newCaddie, req.reason(), manager));

        return AssignmentRes.from(assignment);
    }

    // 배정 취소 — 소프트 삭제로 처리, 같은 팀 재배정 가능
    public void cancelAssignment(Long assignmentId, String reason, AuthenticatedUser auth) {
        validateManager(auth);
        User manager = findUser(auth.getUserId());

        Assignment assignment = findAssignment(assignmentId);
        validateGolfCourseAccess(assignment.getGolfCourse().getId(), auth);
        validateNotCompleted(assignment);

        Caddie caddie = assignment.getCaddie();
        assignment.cancel();
        historyRepository.save(AssignmentHistory.record(
                assignment, assignment.getGolfCourse(), AssignmentChangeType.CANCEL,
                caddie, null, reason, manager));
    }

    // 우천취소 반영 — 라운드하지 못한 배정을 취소하고 정책에 따라 캐디 순번 처리 (FR-513/514)
    // KEEP_ORDER: 순번 유지(재대기) / RESEQUENCE: 순번 소진 처리(당일 대기열 맨 뒤로 이동)
    public RainCancellationRes applyRainCancellation(RainCancellationReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = targetGolfCourseId(auth);
        GolfCourse golfCourse = findGolfCourse(golfCourseId);
        User manager = findUser(auth.getUserId());

        ReservationTeam team = findTeamWithAccess(req.reservationTeamId(), auth);

        Optional<Assignment> activeOpt =
                assignmentRepository.findByReservationTeam_IdAndIsDeletedFalse(team.getId());
        if (activeOpt.isEmpty()) {
            return new RainCancellationRes("해당 팀에 활성 배정이 없어 순번 처리 없이 종료합니다.", false);
        }

        Assignment assignment = activeOpt.get();
        validateNotCompleted(assignment);
        Caddie caddie = assignment.getCaddie();
        LocalDate date = assignment.getAssignmentDate();

        assignment.cancel();
        historyRepository.save(AssignmentHistory.record(
                assignment, golfCourse, AssignmentChangeType.CANCEL,
                caddie, null, "우천취소 반영", manager));

        RainCancellationPolicyType policyType = rainCancellationPolicyRepository
                .findByGolfCourse_IdAndIsDeletedFalse(golfCourseId)
                .map(RainCancellationPolicy::getPolicyType)
                .orElse(RainCancellationPolicyType.KEEP_ORDER);

        if (policyType == RainCancellationPolicyType.RESEQUENCE) {
            // 순번 소진 처리 — 당일 대기열 맨 뒤 번호로 이동
            boolean moved = queueRepository
                    .findByCaddie_IdAndQueueDateAndIsDeletedFalse(caddie.getId(), date)
                    .map(queue -> {
                        int maxNumber = queueRepository
                                .findByGolfCourse_IdAndQueueDateAndIsDeletedFalseOrderByQueueNumberAsc(golfCourseId, date)
                                .stream()
                                .mapToInt(CaddieQueue::getQueueNumber)
                                .max()
                                .orElse(queue.getQueueNumber());
                        int before = queue.getQueueNumber();
                        queue.adjustNumber(maxNumber + 1);
                        queueHistoryRepository.save(CaddieQueueHistory.record(
                                caddie, golfCourse, date, QueueChangeType.MANUAL_ADJUST,
                                before, maxNumber + 1, "우천취소 순번 재정렬", manager));
                        return true;
                    })
                    .orElse(false);
            String message = moved
                    ? "우천취소 반영 완료 — 캐디 순번을 당일 대기열 맨 뒤로 이동했습니다."
                    : "우천취소 반영 완료 — 당일 대기열에 캐디가 없어 순번 이동을 생략했습니다.";
            return new RainCancellationRes(message, moved);
        }

        return new RainCancellationRes("우천취소 반영 완료 — 캐디 순번을 유지합니다(재대기).", true);
    }

    // 배정 검증 — 중복 배정(DUPLICATE) / 휴무자 배정(OFF_DUTY) 검증 (FR-515/516)
    @Transactional(readOnly = true)
    public List<ValidationErrorRes> validateAssignments(LocalDate date, String type, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = targetGolfCourseId(auth);

        List<Assignment> assignments =
                assignmentRepository.findByGolfCourseAndDateWithDetails(golfCourseId, date);
        List<ValidationErrorRes> errors = new ArrayList<>();

        boolean checkDuplicate = type == null || "DUPLICATE".equalsIgnoreCase(type);
        boolean checkOffDuty = type == null || "OFF_DUTY".equalsIgnoreCase(type);

        if (checkDuplicate) {
            int dailyMax = resolveDailyMaxAssignments(golfCourseId, date);
            Map<Long, List<Assignment>> byCaddie = assignments.stream()
                    .collect(Collectors.groupingBy(a -> a.getCaddie().getId()));
            byCaddie.forEach((caddieId, list) -> {
                String caddieName = list.get(0).getCaddie().getName();
                if (list.size() > dailyMax) {
                    errors.add(new ValidationErrorRes("DUPLICATE", caddieId, caddieName,
                            "일일 최대 배정 수(" + dailyMax + "건)를 초과해 " + list.size() + "건 배정되었습니다."));
                }
                // 같은 티타임에 동일 캐디가 2팀 이상 배정된 경우
                Map<LocalTime, Long> timeCounts = list.stream().collect(Collectors.groupingBy(
                        a -> a.getReservationTeam().getTeeTime().getStartTime(), Collectors.counting()));
                timeCounts.forEach((time, count) -> {
                    if (count > 1) {
                        errors.add(new ValidationErrorRes("DUPLICATE", caddieId, caddieName,
                                time + " 티타임에 " + count + "건 중복 배정되었습니다."));
                    }
                });
            });
        }

        if (checkOffDuty) {
            Set<Long> excludedIds = getExcludedCaddieIds(golfCourseId, date);
            assignments.stream()
                    .filter(a -> excludedIds.contains(a.getCaddie().getId()))
                    .forEach(a -> errors.add(new ValidationErrorRes("OFF_DUTY",
                            a.getCaddie().getId(), a.getCaddie().getName(),
                            "휴무/결근/배정제외 상태 캐디가 배정되었습니다.")));
        }

        return errors;
    }

    // 당일 큐 순번 교환 (SWAP) — 두 캐디의 queueNumber만 교환
    // rotation cursor에는 영향 없음 (ADR-005 Decision 6)
    public void swapQueue(SwapQueueReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = targetGolfCourseId(auth);
        GolfCourse golfCourse = findGolfCourse(golfCourseId);
        User manager = findUser(auth.getUserId());

        // 같은 날짜 큐 전체 락 — 순번 교환 중 다른 트랜잭션의 수정 방지
        queueRepository.findForUpdateByGolfCourseAndDate(golfCourseId, req.queueDate());

        CaddieQueue queueA = queueRepository.findByCaddie_IdAndQueueDateAndIsDeletedFalse(
                req.caddieAId(), req.queueDate())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        CaddieQueue queueB = queueRepository.findByCaddie_IdAndQueueDateAndIsDeletedFalse(
                req.caddieBId(), req.queueDate())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        validateGolfCourseAccess(queueA.getGolfCourse().getId(), auth);

        int numberA = queueA.getQueueNumber();
        int numberB = queueB.getQueueNumber();

        queueA.adjustNumber(numberB);
        queueB.adjustNumber(numberA);

        queueHistoryRepository.save(CaddieQueueHistory.record(
                queueA.getCaddie(), golfCourse, req.queueDate(),
                QueueChangeType.SWAP, numberA, numberB, null, manager));
        queueHistoryRepository.save(CaddieQueueHistory.record(
                queueB.getCaddie(), golfCourse, req.queueDate(),
                QueueChangeType.SWAP, numberB, numberA, null, manager));
    }

    // 지정 캐디 잠금 강제 해제 (FR-512) — 사유 필수
    public AssignmentRes unlock(Long assignmentId, UnlockAssignmentReq req, AuthenticatedUser auth) {
        validateManager(auth);
        User manager = findUser(auth.getUserId());

        Assignment assignment = findAssignment(assignmentId);
        validateGolfCourseAccess(assignment.getGolfCourse().getId(), auth);

        if (!assignment.getIsLocked()) {
            throw new BusinessException(AssignmentErrorCode.INVALID_ASSIGNMENT_STATUS);
        }

        Caddie caddie = assignment.getCaddie();
        assignment.unlock();
        historyRepository.save(AssignmentHistory.record(
                assignment, assignment.getGolfCourse(), AssignmentChangeType.UNLOCK,
                caddie, caddie, req.reason(), manager));

        return AssignmentRes.from(assignment);
    }

    // 배정 교환 — 두 배정의 캐디를 맞바꿈 (API-506, post-assignment swap)
    public void swapAssignments(SwapAssignmentReq req, AuthenticatedUser auth) {
        validateManager(auth);
        User manager = findUser(auth.getUserId());

        Assignment a1 = findAssignment(req.assignmentId1());
        Assignment a2 = findAssignment(req.assignmentId2());
        validateGolfCourseAccess(a1.getGolfCourse().getId(), auth);
        validateGolfCourseAccess(a2.getGolfCourse().getId(), auth);
        validateNotCompleted(a1);
        validateNotCompleted(a2);

        Caddie caddie1 = a1.getCaddie();
        Caddie caddie2 = a2.getCaddie();

        a1.reassign(caddie2);
        a2.reassign(caddie1);

        historyRepository.save(AssignmentHistory.record(
                a1, a1.getGolfCourse(), AssignmentChangeType.SWAP, caddie1, caddie2, req.reason(), manager));
        historyRepository.save(AssignmentHistory.record(
                a2, a2.getGolfCourse(), AssignmentChangeType.SWAP, caddie2, caddie1, req.reason(), manager));
    }

    // 미배정 예약팀 조회 — 당일 배정이 없는 RESERVED 상태 팀 (FR-517)
    @Transactional(readOnly = true)
    public List<UnassignedTeamRes> getUnassignedTeams(Long golfCourseId, LocalDate date, AuthenticatedUser auth) {
        validateManager(auth);
        Long targetId = targetGolfCourseId(auth);

        Set<Long> assignedTeamIds = assignmentRepository.findByGolfCourseAndDateWithDetails(targetId, date)
                .stream()
                .map(a -> a.getReservationTeam().getId())
                .collect(Collectors.toSet());

        return reservationTeamRepository.findByGolfCourseIdAndPlayDate(targetId, date)
                .stream()
                .filter(t -> !assignedTeamIds.contains(t.getId()))
                .filter(t -> t.getStatus() == com.fairwaygms.fairwaygmsbe.operation.domain.enums.ReservationTeamStatus.RESERVED)
                .map(UnassignedTeamRes::from)
                .toList();
    }

    // 단건 배정 완료 처리 (FR-522)
    public AssignmentRes completeAssignment(Long assignmentId, AuthenticatedUser auth) {
        validateManager(auth);
        Assignment assignment = findAssignment(assignmentId);
        validateGolfCourseAccess(assignment.getGolfCourse().getId(), auth);

        if (assignment.getStatus() != AssignmentStatus.CONFIRMED) {
            throw new BusinessException(AssignmentErrorCode.INVALID_ASSIGNMENT_STATUS);
        }

        assignment.complete();
        return AssignmentRes.from(assignment);
    }

    // 그룹 캐디를 큐 순번 기준으로 정렬하여 반환
    private List<Caddie> buildGroupCaddiePool(Long caddieGroupId, LocalDate date, Long golfCourseId) {
        List<CaddieQueue> groupQueue = queueRepository
                .findByGolfCourse_IdAndQueueDateAndIsDeletedFalseOrderByQueueNumberAsc(golfCourseId, date)
                .stream()
                .filter(q -> {
                    CaddieGroup g = q.getCaddie().getCaddieGroup();
                    return g != null && g.getId().equals(caddieGroupId);
                })
                .collect(Collectors.toList());

        if (!groupQueue.isEmpty()) {
            return groupQueue.stream().map(CaddieQueue::getCaddie).collect(Collectors.toList());
        }

        // 큐가 없으면 caddieNumber 순 fallback
        return caddieRepository.findByGolfCourse_IdAndStatusAndIsDeletedFalse(
                        golfCourseId, com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieStatus.ACTIVE)
                .stream()
                .filter(c -> c.getCaddieGroup() != null && c.getCaddieGroup().getId().equals(caddieGroupId))
                .sorted(Comparator.comparing(c -> c.getCaddieNumber() != null ? c.getCaddieNumber() : ""))
                .collect(Collectors.toList());
    }

    private Set<Long> getAlreadyAssignedTeamIds(Long golfCourseId, LocalDate date) {
        return assignmentRepository.findByGolfCourseAndDateWithDetails(golfCourseId, date)
                .stream()
                .map(a -> a.getReservationTeam().getId())
                .collect(Collectors.toSet());
    }

    private Map<Long, Integer> buildCaddieDayCountMap(Long golfCourseId, LocalDate date) {
        return assignmentRepository.findByGolfCourseAndDateWithDetails(golfCourseId, date)
                .stream()
                .collect(Collectors.groupingBy(
                        a -> a.getCaddie().getId(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
    }

    private Set<Long> getExcludedCaddieIds(Long golfCourseId, LocalDate date) {
        return caddieDailyStatusRepository.findByGolfCourse_IdAndStatusDateAndIsDeletedFalse(golfCourseId, date)
                .stream()
                .filter(s -> EXCLUDED_STATUSES.contains(s.getType()))
                .map(s -> s.getCaddie().getId())
                .collect(Collectors.toSet());
    }

    private void updateRotationStates(GolfCourse golfCourse, Map<Long, Caddie> lastAssignedByGroup) {
        for (Map.Entry<Long, Caddie> entry : lastAssignedByGroup.entrySet()) {
            Long groupId = entry.getKey();
            Caddie lastCaddie = entry.getValue();

            // 그룹 내 캐디를 caddieNumber 순으로 정렬
            List<Caddie> groupCaddies = caddieRepository
                    .findByGolfCourse_IdAndStatusAndIsDeletedFalse(
                            golfCourse.getId(), com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieStatus.ACTIVE)
                    .stream()
                    .filter(c -> c.getCaddieGroup() != null && c.getCaddieGroup().getId().equals(groupId))
                    .sorted(Comparator.comparing(c -> c.getCaddieNumber() != null ? c.getCaddieNumber() : ""))
                    .collect(Collectors.toList());

            if (groupCaddies.isEmpty()) continue;

            int lastIdx = IntStream.range(0, groupCaddies.size())
                    .filter(i -> groupCaddies.get(i).getId().equals(lastCaddie.getId()))
                    .findFirst().orElse(-1);
            if (lastIdx < 0) continue;

            // 마지막 배정 캐디 다음 캐디가 내일 또는 다음 부의 시작 캐디
            Caddie nextStart = groupCaddies.get((lastIdx + 1) % groupCaddies.size());

            QueueRotationState state = rotationStateRepository
                    .findByGolfCourse_IdAndCaddieGroup_Id(golfCourse.getId(), groupId)
                    .orElseGet(() -> {
                        CaddieGroup group = caddieGroupRepository.findById(groupId).orElseThrow();
                        return rotationStateRepository.save(QueueRotationState.create(golfCourse, group));
                    });
            state.updateNextStart(nextStart);
        }
    }

    private void validateDailyLimit(Long golfCourseId, Long caddieId, LocalDate date, boolean isHalfBack) {
        int currentCount = assignmentRepository.countByCaddie_IdAndAssignmentDateAndIsDeletedFalse(caddieId, date);
        int maxAllowed = isHalfBack ? resolveDailyMaxAssignments(golfCourseId, date) : 1;
        if (currentCount >= maxAllowed) {
            throw new BusinessException(AssignmentErrorCode.CADDIE_ASSIGNMENT_LIMIT_EXCEEDED);
        }
    }

    // 일일 최대 배정 수 = 그날 운영 부 수 (3부제면 3근무 허용)
    // 1부제여도 하프백(동시 2팀) 케이스가 있으므로 최소 2는 보장
    private int resolveDailyMaxAssignments(Long golfCourseId, LocalDate date) {
        String yearMonth = date.format(YEAR_MONTH_FMT);
        int periodCount = operationSettingRepository
                .findByGolfCourse_IdAndYearMonthAndIsDeletedFalse(golfCourseId, yearMonth)
                .map(setting -> operationPeriodRepository
                        .findByOperationSetting_IdAndIsActiveTrueAndIsDeletedFalse(setting.getId())
                        .stream()
                        .map(OperationPeriod::getPeriodNumber)
                        .collect(Collectors.toSet())
                        .size())
                .orElse(0);
        return Math.max(periodCount, DEFAULT_MAX_DAILY_ASSIGNMENTS);
    }

    private void validateNotCompleted(Assignment assignment) {
        if (assignment.getStatus() == AssignmentStatus.COMPLETED) {
            throw new BusinessException(AssignmentErrorCode.INVALID_ASSIGNMENT_STATUS);
        }
    }

    private Assignment findAssignment(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND));
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

    private void validateGolfCourseAccess(Long resourceGolfCourseId, AuthenticatedUser auth) {
        if (auth.isAdmin()) return;
        if (!resourceGolfCourseId.equals(targetGolfCourseId(auth))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
