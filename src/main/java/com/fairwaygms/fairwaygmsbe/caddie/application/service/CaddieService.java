package com.fairwaygms.fairwaygmsbe.caddie.application.service;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.auth.exception.AuthErrorCode;
import com.fairwaygms.fairwaygmsbe.assignment.domain.enums.AssignmentStatus;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentRepository;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.ChangeCaddieStatusReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.CreateCaddieReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.LinkAccountReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.RoundCompleteReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.UpdateCaddieReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.UpdateWorkPatternReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.AvailableCaddieRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.CaddieRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.CaddieWithdrawRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.RoundCompleteRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.WorkPatternRes;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieQueue;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieWorkPattern;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieStatus;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.DailyStatusType;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieDailyStatusRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieQueueRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieWorkPatternRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CaddieService {

    // 퇴사 처리 시 자동배정 제외 기준 유형
    private static final Set<DailyStatusType> EXCLUDE_TYPES =
            Set.of(DailyStatusType.DAY_OFF, DailyStatusType.ABSENCE, DailyStatusType.ASSIGN_EXCLUDED);

    private final CaddieRepository caddieRepository;
    private final AssignmentRepository assignmentRepository;
    private final CaddieWorkPatternRepository workPatternRepository;
    private final CaddieDailyStatusRepository dailyStatusRepository;
    private final CaddieQueueRepository queueRepository;
    private final GolfCourseRepository golfCourseRepository;
    private final UserRepository userRepository;
    private final com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver contextResolver;

    // ADMIN은 X-Selected-Golf-Course-Id 헤더의 선택 골프장, MANAGER는 소속 골프장을 대상으로 한다
    private Long targetGolfCourseId(AuthenticatedUser auth) {
        return auth.isAdmin() ? contextResolver.resolveTargetGolfCourseId(auth) : auth.getGolfCourseId();
    }

    // FR-301: Caddy 가입 승인 시 auth 도메인에서 호출 — 기본 근무 패턴도 함께 생성
    public Caddie createOnApproval(User user) {
        GolfCourse golfCourse = findGolfCourse(user.getGolfCourseId());
        Caddie caddie = Caddie.createOnApproval(golfCourse, user, user.getName());
        caddieRepository.save(caddie);
        CaddieWorkPattern pattern = CaddieWorkPattern.createDefault(caddie, golfCourse);
        workPatternRepository.save(pattern);
        return caddie;
    }

    // API-301 (FR-301): 캐디 직접 등록 — 계정 없는 캐디 등록, 계정은 이후 연동 API로 연결
    public CaddieRes createCaddie(CreateCaddieReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = targetGolfCourseId(auth);
        GolfCourse golfCourse = findGolfCourse(golfCourseId);

        if (caddieRepository.existsByGolfCourse_IdAndCaddieNumberAndIsDeletedFalse(
                golfCourseId, req.caddieNumber())) {
            throw new BusinessException(CaddieErrorCode.DUPLICATE_CADDIE_NUMBER);
        }

        Caddie caddie = Caddie.createDirect(golfCourse, req.caddieNumber(), req.name(),
                req.phone(), req.hireDate());
        caddieRepository.save(caddie);

        // 승인 생성 흐름과 동일하게 기본 근무 패턴 함께 생성
        workPatternRepository.save(CaddieWorkPattern.createDefault(caddie, golfCourse));
        return CaddieRes.from(caddie);
    }

    // API-306 (FR-306): 캐디-계정 연동 — 직접 등록된 캐디에 Caddy 계정을 연결
    public CaddieRes linkAccount(Long caddieId, LinkAccountReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Caddie caddie = findCaddie(caddieId);
        validateGolfCourseAccess(caddie.getGolfCourse().getId(), auth);

        if (caddie.getUser() != null) {
            throw new BusinessException(CaddieErrorCode.CADDIE_ALREADY_LINKED);
        }

        User user = userRepository.findByIdAndIsDeletedFalse(req.userId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        // 다른 골프장 계정 또는 CADDY 이외 역할은 연동 불가
        if (user.getRole() != UserRole.CADDY
                || !caddie.getGolfCourse().getId().equals(user.getGolfCourseId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        // 해당 계정이 이미 다른 캐디와 연동된 경우
        if (caddieRepository.findByUser_IdAndIsDeletedFalse(user.getId()).isPresent()) {
            throw new BusinessException(CaddieErrorCode.CADDIE_ALREADY_LINKED);
        }

        caddie.linkAccount(user);
        return CaddieRes.from(caddie);
    }

    // API-314 (FR-315/316): 라운딩 완료 처리 — 당일 확정 배정 중 가장 이른 건을 완료하고 복귀 시간 기록
    public RoundCompleteRes completeRound(Long caddieId, RoundCompleteReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Caddie caddie = findCaddie(caddieId);
        validateGolfCourseAccess(caddie.getGolfCourse().getId(), auth);

        LocalDateTime completedAt = req != null && req.completedAt() != null
                ? req.completedAt() : LocalDateTime.now();

        // 복귀 시간 기준으로 당일 CONFIRMED 배정을 완료 처리 — 없으면 상태만 응답 (재대기 판단용)
        boolean assignmentCompleted = assignmentRepository
                .findConfirmedByCaddieAndDate(caddieId, completedAt.toLocalDate())
                .stream()
                .filter(a -> a.getStatus() == AssignmentStatus.CONFIRMED)
                .findFirst()
                .map(a -> {
                    a.complete();
                    return true;
                })
                .orElse(false);

        return new RoundCompleteRes(caddieId, caddie.getStatus().name(), completedAt, assignmentCompleted);
    }

    // FR-302: 골프장별 캐디 목록 — ADMIN은 golfCourseId 직접 전달, MANAGER는 소속 골프장만
    @Transactional(readOnly = true)
    public List<CaddieRes> getList(Long golfCourseId, AuthenticatedUser auth) {
        validateManager(auth); // 관리 조회는 MANAGER/ADMIN 전용 — CADDY 접근 차단
        Long targetId = resolveGolfCourseId(golfCourseId, auth);
        return caddieRepository.findByGolfCourse_IdAndIsDeletedFalse(targetId)
                .stream()
                .map(CaddieRes::from)
                .toList();
    }

    // FR-303: 캐디 상세 조회
    @Transactional(readOnly = true)
    public CaddieRes getDetail(Long caddieId, AuthenticatedUser auth) {
        validateManager(auth); // 관리 조회는 MANAGER/ADMIN 전용 — CADDY 접근 차단
        Caddie caddie = findCaddie(caddieId);
        validateGolfCourseAccess(caddie.getGolfCourse().getId(), auth);
        return CaddieRes.from(caddie);
    }

    // FR-304: 캐디 정보 수정 (번호, 연락처, 입사일) — MANAGER 전용
    public CaddieRes updateInfo(Long caddieId, UpdateCaddieReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Caddie caddie = findCaddie(caddieId);
        validateGolfCourseAccess(caddie.getGolfCourse().getId(), auth);

        // 캐디 번호 변경 시 동일 골프장 내 중복 여부 확인
        boolean numberChanged = !caddie.getCaddieNumber().equals(req.caddieNumber());
        if (numberChanged && caddieRepository.existsByGolfCourse_IdAndCaddieNumberAndIsDeletedFalse(
                caddie.getGolfCourse().getId(), req.caddieNumber())) {
            throw new BusinessException(CaddieErrorCode.DUPLICATE_CADDIE_NUMBER);
        }

        caddie.updateInfo(req.caddieNumber(), req.phone(), req.hireDate());
        return CaddieRes.from(caddie);
    }

    // FR-305: 캐디 상태 변경 (재직/휴직/일시제외) — RESIGNED는 withdrawCaddie() 사용
    public CaddieRes changeStatus(Long caddieId, ChangeCaddieStatusReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Caddie caddie = findCaddie(caddieId);
        validateGolfCourseAccess(caddie.getGolfCourse().getId(), auth);

        // 퇴사는 withdrawCaddie()를 통해서만 처리 — user 계정 동시 처리 필요
        if (req.status() == CaddieStatus.RESIGNED) {
            throw new BusinessException(CaddieErrorCode.INVALID_CADDIE_STATUS,
                    "퇴사 처리는 /withdraw API를 사용해야 합니다.");
        }
        caddie.changeStatus(req.status());
        return CaddieRes.from(caddie);
    }

    // FR-118: 캐디 퇴사 처리 — caddie.RESIGNED + user.WITHDRAWN 동시 처리
    public CaddieWithdrawRes withdrawCaddie(Long caddieId, AuthenticatedUser auth) {
        validateManager(auth);
        Caddie caddie = findCaddie(caddieId);
        validateGolfCourseAccess(caddie.getGolfCourse().getId(), auth);

        caddie.resign();

        // 연동된 user 계정도 함께 WITHDRAWN 처리
        if (caddie.getUser() != null) {
            User user = userRepository.findByIdAndIsDeletedFalse(caddie.getUser().getId())
                    .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
            user.withdraw();
        }
        return CaddieWithdrawRes.from(caddie);
    }

    // FR-324: 가용 캐디 조회 — ACTIVE 상태이며 해당 날짜에 배정 제외 유형이 없는 캐디
    @Transactional(readOnly = true)
    public List<AvailableCaddieRes> getAvailableCaddies(Long golfCourseId, LocalDate date, AuthenticatedUser auth) {
        validateManager(auth); // 관리 조회는 MANAGER/ADMIN 전용 — CADDY 접근 차단
        Long targetId = resolveGolfCourseId(golfCourseId, auth);
        List<Caddie> activeCaddies = caddieRepository
                .findByGolfCourse_IdAndStatusAndIsDeletedFalse(targetId, CaddieStatus.ACTIVE);

        // 해당 날짜의 순번 맵 (caddieId → queueNumber)
        Map<Long, Integer> queueMap = queueRepository
                .findByGolfCourse_IdAndQueueDateAndIsDeletedFalseOrderByQueueNumberAsc(targetId, date)
                .stream()
                .collect(Collectors.toMap(q -> q.getCaddie().getId(), CaddieQueue::getQueueNumber));

        return activeCaddies.stream()
                .filter(c -> isAvailable(c.getId(), targetId, date))
                .map(c -> AvailableCaddieRes.of(c, queueMap.get(c.getId())))
                .toList();
    }

    // FR-310/311: 근무 패턴 수정 (주중/주말 가능 여부, 부 제한, 첫대기 수동 여부)
    public WorkPatternRes updateWorkPattern(Long caddieId, UpdateWorkPatternReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Caddie caddie = findCaddie(caddieId);
        validateGolfCourseAccess(caddie.getGolfCourse().getId(), auth);

        CaddieWorkPattern pattern = workPatternRepository.findByCaddie_IdAndIsDeletedFalse(caddieId)
                .orElseThrow(() -> new BusinessException(CaddieErrorCode.WORK_PATTERN_NOT_FOUND));

        pattern.update(req.canWeekday(), req.canWeekend(), req.periodLimit(), req.isFirstWaitManual());
        return WorkPatternRes.from(pattern);
    }

    // 해당 날짜에 배정 제외 유형(휴무/결근/배정제외)이 하나도 없으면 가용 상태
    private boolean isAvailable(Long caddieId, Long golfCourseId, LocalDate date) {
        return EXCLUDE_TYPES.stream().noneMatch(type ->
                dailyStatusRepository.existsByCaddie_IdAndStatusDateAndTypeAndIsDeletedFalse(caddieId, date, type));
    }

    private Caddie findCaddie(Long caddieId) {
        return caddieRepository.findByIdAndIsDeletedFalse(caddieId)
                .orElseThrow(() -> new BusinessException(CaddieErrorCode.CADDIE_NOT_FOUND));
    }

    private GolfCourse findGolfCourse(Long golfCourseId) {
        return golfCourseRepository.findByIdAndIsDeletedFalse(golfCourseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOLF_COURSE_NOT_FOUND));
    }

    private void validateManager(AuthenticatedUser auth) {
        if (auth.getRole() != UserRole.MANAGER && !auth.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private Long resolveGolfCourseId(Long requestedId, AuthenticatedUser auth) {
        if (auth.isAdmin()) {
            return requestedId;
        }
        // MANAGER/CADDY는 JWT claim의 소속 골프장만 접근 가능
        return targetGolfCourseId(auth);
    }

    private void validateGolfCourseAccess(Long caddieGolfCourseId, AuthenticatedUser auth) {
        if (auth.isAdmin()) return;
        if (!caddieGolfCourseId.equals(targetGolfCourseId(auth))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
