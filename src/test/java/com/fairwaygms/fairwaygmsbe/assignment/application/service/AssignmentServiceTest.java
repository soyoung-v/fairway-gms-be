package com.fairwaygms.fairwaygmsbe.assignment.application.service;

import com.fairwaygms.fairwaygmsbe.assignment.application.model.req.*;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.AutoAssignRes;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.AssignmentHistory;
import com.fairwaygms.fairwaygmsbe.assignment.domain.enums.AssignmentStatus;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentHistoryRepository;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentRepository;
import com.fairwaygms.fairwaygmsbe.assignment.exception.AssignmentErrorCode;
import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieGroup;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieDailyStatus;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieQueue;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.QueueRotationState;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieGroupAssignmentType;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.DailyStatusType;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.*;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.ReservationTeam;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.TeeTime;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.ReservationTeamRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.TeeTimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssignmentServiceTest {

    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentHistoryRepository historyRepository;
    @Mock private com.fairwaygms.fairwaygmsbe.assignment.domain.repository.CartAssignmentRepository cartAssignmentRepository;
    @Mock private ReservationTeamRepository reservationTeamRepository;
    @Mock private CaddieRepository caddieRepository;
    @Mock private CaddieQueueRepository queueRepository;
    @Mock private CaddieQueueHistoryRepository queueHistoryRepository;
    @Mock private CaddieGroupRepository caddieGroupRepository;
    @Mock private QueueRotationStateRepository rotationStateRepository;
    @Mock private CaddieDailyStatusRepository caddieDailyStatusRepository;
    @Mock private TeeTimeRepository teeTimeRepository;
    @Mock private com.fairwaygms.fairwaygmsbe.operation.domain.repository.RainCancellationPolicyRepository rainCancellationPolicyRepository;
    @Mock private com.fairwaygms.fairwaygmsbe.operation.domain.repository.OperationSettingRepository operationSettingRepository;
    @Mock private com.fairwaygms.fairwaygmsbe.operation.domain.repository.OperationPeriodRepository operationPeriodRepository;
    @Mock private GolfCourseRepository golfCourseRepository;
    @Mock private UserRepository userRepository;
    @Mock private com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver contextResolver;

    private AssignmentService assignmentService;

    private static final Long GOLF_COURSE_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 7, 1);

    private GolfCourse golfCourse;
    private User manager;
    private AuthenticatedUser managerAuth;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentService(
                assignmentRepository, historyRepository, cartAssignmentRepository,
                reservationTeamRepository, caddieRepository, queueRepository,
                queueHistoryRepository, caddieGroupRepository, rotationStateRepository,
                caddieDailyStatusRepository, teeTimeRepository,
                rainCancellationPolicyRepository,
                operationSettingRepository, operationPeriodRepository,
                golfCourseRepository, userRepository, contextResolver
        );

        golfCourse = mockGolfCourse(GOLF_COURSE_ID);
        manager = mock(User.class);
        managerAuth = new AuthenticatedUser(10L, UserRole.MANAGER, GOLF_COURSE_ID);
    }

    // ─── autoAssign ──────────────────────────────────────────────────────────

    @Test
    void autoAssign_큐가_없으면_예외() {
        // given
        when(golfCourseRepository.findByIdAndIsDeletedFalse(GOLF_COURSE_ID)).thenReturn(Optional.of(golfCourse));
        when(userRepository.findByIdAndIsDeletedFalse(anyLong())).thenReturn(Optional.of(manager));
        when(assignmentRepository.findForUpdateByGolfCourseAndDate(eq(GOLF_COURSE_ID), eq(DATE)))
                .thenReturn(List.of());
        when(queueRepository.findForUpdateByGolfCourseAndDate(eq(GOLF_COURSE_ID), eq(DATE)))
                .thenReturn(List.of());

        AutoAssignReq req = new AutoAssignReq(DATE, 1L, null);

        // when / then
        assertThatThrownBy(() -> assignmentService.autoAssign(req, managerAuth))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(AssignmentErrorCode.CADDIE_QUEUE_EMPTY.getMessage());
    }

    @Test
    void autoAssign_DAY_OFF_캐디는_배정풀에서_제외() {
        // given
        Caddie caddieA = mockCaddie(1L, "C001");
        Caddie caddieB = mockCaddie(2L, "C002");
        CaddieQueue queueA = mockCaddieQueue(caddieA, 1);
        CaddieQueue queueB = mockCaddieQueue(caddieB, 2);
        ReservationTeam team = mockReservationTeam(100L);

        when(golfCourseRepository.findByIdAndIsDeletedFalse(GOLF_COURSE_ID)).thenReturn(Optional.of(golfCourse));
        when(userRepository.findByIdAndIsDeletedFalse(anyLong())).thenReturn(Optional.of(manager));
        when(assignmentRepository.findForUpdateByGolfCourseAndDate(eq(GOLF_COURSE_ID), eq(DATE)))
                .thenReturn(List.of());
        when(queueRepository.findForUpdateByGolfCourseAndDate(eq(GOLF_COURSE_ID), eq(DATE)))
                .thenReturn(List.of(queueA, queueB));

        // caddieA는 DAY_OFF 상태
        CaddieDailyStatus dayOff = mockDailyStatus(caddieA, DailyStatusType.DAY_OFF);
        when(caddieDailyStatusRepository.findByGolfCourse_IdAndStatusDateAndIsDeletedFalse(GOLF_COURSE_ID, DATE))
                .thenReturn(List.of(dayOff));
        when(reservationTeamRepository.findByPeriodIdAndPlayDate(anyLong(), eq(DATE)))
                .thenReturn(List.of(team));
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AutoAssignReq req = new AutoAssignReq(DATE, 1L, null);

        // when
        AutoAssignRes result = assignmentService.autoAssign(req, managerAuth);

        // then — caddieB만 배정되어야 함
        assertThat(result.assignedCount()).isEqualTo(1);
        ArgumentCaptor<Assignment> captor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getCaddie().getId()).isEqualTo(caddieB.getId());
    }

    @Test
    void autoAssign_SESSION_FIXED_그룹_캐디는_풀에서_제외() {
        // given
        CaddieGroup fixedGroup = mockCaddieGroup(99L, CaddieGroupAssignmentType.SESSION_FIXED);
        Caddie fixedCaddie = mockCaddieWithGroup(3L, "C003", fixedGroup);
        Caddie normalCaddie = mockCaddie(4L, "C004");
        CaddieQueue fixedQueue = mockCaddieQueue(fixedCaddie, 1);
        CaddieQueue normalQueue = mockCaddieQueue(normalCaddie, 2);
        ReservationTeam team = mockReservationTeam(101L);

        when(golfCourseRepository.findByIdAndIsDeletedFalse(GOLF_COURSE_ID)).thenReturn(Optional.of(golfCourse));
        when(userRepository.findByIdAndIsDeletedFalse(anyLong())).thenReturn(Optional.of(manager));
        when(assignmentRepository.findForUpdateByGolfCourseAndDate(eq(GOLF_COURSE_ID), eq(DATE)))
                .thenReturn(List.of());
        when(queueRepository.findForUpdateByGolfCourseAndDate(eq(GOLF_COURSE_ID), eq(DATE)))
                .thenReturn(List.of(fixedQueue, normalQueue));
        when(caddieDailyStatusRepository.findByGolfCourse_IdAndStatusDateAndIsDeletedFalse(GOLF_COURSE_ID, DATE))
                .thenReturn(List.of());
        when(reservationTeamRepository.findByPeriodIdAndPlayDate(anyLong(), eq(DATE)))
                .thenReturn(List.of(team));
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AutoAssignReq req = new AutoAssignReq(DATE, 1L, null);

        // when
        AutoAssignRes result = assignmentService.autoAssign(req, managerAuth);

        // then — SESSION_FIXED 캐디 제외하고 일반 캐디만 배정
        assertThat(result.assignedCount()).isEqualTo(1);
        ArgumentCaptor<Assignment> captor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getCaddie().getId()).isEqualTo(normalCaddie.getId());
    }

    @Test
    void autoAssign_두번째_배정은_isHalfBack_true() {
        // given
        Caddie caddie = mockCaddie(5L, "C005");
        CaddieQueue queue = mockCaddieQueue(caddie, 1);

        // 이미 1건 배정 존재
        Assignment existing = mockExistingAssignment(50L, caddie, false);
        ReservationTeam team = mockReservationTeam(102L);

        when(golfCourseRepository.findByIdAndIsDeletedFalse(GOLF_COURSE_ID)).thenReturn(Optional.of(golfCourse));
        when(userRepository.findByIdAndIsDeletedFalse(anyLong())).thenReturn(Optional.of(manager));
        when(assignmentRepository.findForUpdateByGolfCourseAndDate(eq(GOLF_COURSE_ID), eq(DATE)))
                .thenReturn(List.of(existing));
        when(queueRepository.findForUpdateByGolfCourseAndDate(eq(GOLF_COURSE_ID), eq(DATE)))
                .thenReturn(List.of(queue));
        when(caddieDailyStatusRepository.findByGolfCourse_IdAndStatusDateAndIsDeletedFalse(GOLF_COURSE_ID, DATE))
                .thenReturn(List.of());
        when(reservationTeamRepository.findByPeriodIdAndPlayDate(anyLong(), eq(DATE)))
                .thenReturn(List.of(team));
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AutoAssignReq req = new AutoAssignReq(DATE, 1L, null);

        // when
        AutoAssignRes result = assignmentService.autoAssign(req, managerAuth);

        // then — 두 번째 배정은 isHalfBack=true
        assertThat(result.assignedCount()).isEqualTo(1);
        ArgumentCaptor<Assignment> captor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getIsHalfBack()).isTrue();
    }

    // ─── swapQueue ───────────────────────────────────────────────────────────

    @Test
    void swapQueue_두_캐디의_큐_순번이_교환된다() {
        // given
        Caddie caddieA = mockCaddie(10L, "C010");
        Caddie caddieB = mockCaddie(11L, "C011");
        CaddieQueue queueA = CaddieQueue.create(caddieA, golfCourse, DATE, 1);
        CaddieQueue queueB = CaddieQueue.create(caddieB, golfCourse, DATE, 3);

        when(golfCourseRepository.findByIdAndIsDeletedFalse(GOLF_COURSE_ID)).thenReturn(Optional.of(golfCourse));
        when(userRepository.findByIdAndIsDeletedFalse(anyLong())).thenReturn(Optional.of(manager));
        when(queueRepository.findForUpdateByGolfCourseAndDate(eq(GOLF_COURSE_ID), eq(DATE)))
                .thenReturn(List.of(queueA, queueB));
        when(queueRepository.findByCaddie_IdAndQueueDateAndIsDeletedFalse(10L, DATE))
                .thenReturn(Optional.of(queueA));
        when(queueRepository.findByCaddie_IdAndQueueDateAndIsDeletedFalse(11L, DATE))
                .thenReturn(Optional.of(queueB));
        when(queueHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SwapQueueReq req = new SwapQueueReq(10L, 11L, DATE);

        // when
        assignmentService.swapQueue(req, managerAuth);

        // then
        assertThat(queueA.getQueueNumber()).isEqualTo(3);
        assertThat(queueB.getQueueNumber()).isEqualTo(1);
    }

    // ─── swapAssignments ─────────────────────────────────────────────────────

    @Test
    void swapAssignments_두_배정의_캐디가_맞교환된다() {
        // given
        Caddie caddieA = mockCaddie(20L, "C020");
        Caddie caddieB = mockCaddie(21L, "C021");
        Assignment a1 = mockActiveAssignment(200L, caddieA);
        Assignment a2 = mockActiveAssignment(201L, caddieB);

        when(userRepository.findByIdAndIsDeletedFalse(anyLong())).thenReturn(Optional.of(manager));
        when(assignmentRepository.findById(200L)).thenReturn(Optional.of(a1));
        when(assignmentRepository.findById(201L)).thenReturn(Optional.of(a2));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SwapAssignmentReq req = new SwapAssignmentReq(200L, 201L, "순서 조정");

        // when
        assignmentService.swapAssignments(req, managerAuth);

        // then
        assertThat(a1.getCaddie().getId()).isEqualTo(caddieB.getId());
        assertThat(a2.getCaddie().getId()).isEqualTo(caddieA.getId());
        verify(historyRepository, times(2)).save(any(AssignmentHistory.class));
    }

    @Test
    void swapAssignments_완료된_배정은_교환_불가() {
        // given
        Caddie caddieA = mockCaddie(22L, "C022");
        Caddie caddieB = mockCaddie(23L, "C023");
        Assignment completed = mockCompletedAssignment(210L, caddieA);
        Assignment active = mockActiveAssignment(211L, caddieB);

        when(userRepository.findByIdAndIsDeletedFalse(anyLong())).thenReturn(Optional.of(manager));
        when(assignmentRepository.findById(210L)).thenReturn(Optional.of(completed));
        when(assignmentRepository.findById(211L)).thenReturn(Optional.of(active));

        SwapAssignmentReq req = new SwapAssignmentReq(210L, 211L, "test");

        // when / then
        assertThatThrownBy(() -> assignmentService.swapAssignments(req, managerAuth))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(AssignmentErrorCode.INVALID_ASSIGNMENT_STATUS.getMessage());
    }

    // ─── unlock ──────────────────────────────────────────────────────────────

    @Test
    void unlock_잠금_배정을_해제한다() {
        // given
        Caddie caddie = mockCaddie(30L, "C030");
        Assignment locked = mockLockedAssignment(300L, caddie);

        when(userRepository.findByIdAndIsDeletedFalse(anyLong())).thenReturn(Optional.of(manager));
        when(assignmentRepository.findById(300L)).thenReturn(Optional.of(locked));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UnlockAssignmentReq req = new UnlockAssignmentReq("관리자 요청");

        // when
        assignmentService.unlock(300L, req, managerAuth);

        // then
        assertThat(locked.getIsLocked()).isFalse();
        verify(historyRepository).save(any(AssignmentHistory.class));
    }

    @Test
    void unlock_이미_해제된_배정은_예외() {
        // given
        Caddie caddie = mockCaddie(31L, "C031");
        Assignment unlocked = mockActiveAssignment(301L, caddie); // isLocked=false

        when(userRepository.findByIdAndIsDeletedFalse(anyLong())).thenReturn(Optional.of(manager));
        when(assignmentRepository.findById(301L)).thenReturn(Optional.of(unlocked));

        UnlockAssignmentReq req = new UnlockAssignmentReq("test");

        // when / then
        assertThatThrownBy(() -> assignmentService.unlock(301L, req, managerAuth))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(AssignmentErrorCode.INVALID_ASSIGNMENT_STATUS.getMessage());
    }

    // ─── manualPreAssign ─────────────────────────────────────────────────────

    @Test
    void manualPreAssign_이미_배정된_팀에_재배정_시_예외() {
        // given
        ReservationTeam team = mockReservationTeam(400L);
        Caddie caddie = mockCaddie(40L, "C040");

        when(golfCourseRepository.findByIdAndIsDeletedFalse(GOLF_COURSE_ID)).thenReturn(Optional.of(golfCourse));
        when(userRepository.findByIdAndIsDeletedFalse(anyLong())).thenReturn(Optional.of(manager));
        when(reservationTeamRepository.findByIdAndIsDeletedFalse(400L)).thenReturn(Optional.of(team));
        when(assignmentRepository.existsByReservationTeam_IdAndIsDeletedFalse(400L)).thenReturn(true);

        ManualPreAssignReq req = new ManualPreAssignReq(400L, caddie.getId(), false, false, null);

        // when / then
        assertThatThrownBy(() -> assignmentService.manualPreAssign(req, managerAuth))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(AssignmentErrorCode.ASSIGNMENT_ALREADY_EXISTS.getMessage());
    }

    @Test
    void manualPreAssign_isLocked_true로_배정되면_잠금_설정() {
        // given
        ReservationTeam team = mockReservationTeam(401L);
        Caddie caddie = mockCaddie(41L, "C041");

        when(golfCourseRepository.findByIdAndIsDeletedFalse(GOLF_COURSE_ID)).thenReturn(Optional.of(golfCourse));
        when(userRepository.findByIdAndIsDeletedFalse(anyLong())).thenReturn(Optional.of(manager));
        when(reservationTeamRepository.findByIdAndIsDeletedFalse(401L)).thenReturn(Optional.of(team));
        when(assignmentRepository.existsByReservationTeam_IdAndIsDeletedFalse(401L)).thenReturn(false);
        when(caddieRepository.findByIdAndIsDeletedFalse(41L)).thenReturn(Optional.of(caddie));
        when(assignmentRepository.countByCaddie_IdAndAssignmentDateAndIsDeletedFalse(eq(41L), any())).thenReturn(0);
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ManualPreAssignReq req = new ManualPreAssignReq(401L, 41L, true, false, "VIP 고객");

        // when
        var result = assignmentService.manualPreAssign(req, managerAuth);

        // then
        ArgumentCaptor<Assignment> captor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getIsLocked()).isTrue();
    }

    // ─── completeAssignment ──────────────────────────────────────────────────

    @Test
    void completeAssignment_CONFIRMED_상태만_완료_처리_가능() {
        // given
        Caddie caddie = mockCaddie(50L, "C050");
        Assignment assigned = mockActiveAssignment(500L, caddie); // ASSIGNED 상태

        when(assignmentRepository.findById(500L)).thenReturn(Optional.of(assigned));

        // when / then
        assertThatThrownBy(() -> assignmentService.completeAssignment(500L, managerAuth))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(AssignmentErrorCode.INVALID_ASSIGNMENT_STATUS.getMessage());
    }

    @Test
    void completeAssignment_CONFIRMED_배정은_COMPLETED_상태로_변경() {
        // given
        Caddie caddie = mockCaddie(51L, "C051");
        Assignment confirmed = mockConfirmedAssignment(501L, caddie);

        when(assignmentRepository.findById(501L)).thenReturn(Optional.of(confirmed));

        // when
        assignmentService.completeAssignment(501L, managerAuth);

        // then
        assertThat(confirmed.getStatus()).isEqualTo(AssignmentStatus.COMPLETED);
    }

    // ─── 헬퍼 팩토리 ─────────────────────────────────────────────────────────

    private GolfCourse mockGolfCourse(Long id) {
        GolfCourse gc = mock(GolfCourse.class);
        when(gc.getId()).thenReturn(id);
        return gc;
    }

    private Caddie mockCaddie(Long id, String number) {
        Caddie caddie = mock(Caddie.class);
        when(caddie.getId()).thenReturn(id);
        when(caddie.getCaddieNumber()).thenReturn(number);
        when(caddie.getCaddieGroup()).thenReturn(null);
        when(caddie.getGolfCourse()).thenReturn(golfCourse);
        return caddie;
    }

    private Caddie mockCaddieWithGroup(Long id, String number, CaddieGroup group) {
        Caddie caddie = mock(Caddie.class);
        when(caddie.getId()).thenReturn(id);
        when(caddie.getCaddieNumber()).thenReturn(number);
        when(caddie.getCaddieGroup()).thenReturn(group);
        when(caddie.getGolfCourse()).thenReturn(golfCourse);
        return caddie;
    }

    private CaddieGroup mockCaddieGroup(Long id, CaddieGroupAssignmentType type) {
        CaddieGroup group = mock(CaddieGroup.class);
        when(group.getId()).thenReturn(id);
        when(group.getAssignmentType()).thenReturn(type);
        return group;
    }

    private CaddieQueue mockCaddieQueue(Caddie caddie, int number) {
        CaddieQueue queue = mock(CaddieQueue.class);
        when(queue.getCaddie()).thenReturn(caddie);
        when(queue.getQueueNumber()).thenReturn(number);
        when(queue.getGolfCourse()).thenReturn(golfCourse);
        return queue;
    }

    private CaddieDailyStatus mockDailyStatus(Caddie caddie, DailyStatusType type) {
        CaddieDailyStatus status = mock(CaddieDailyStatus.class);
        when(status.getCaddie()).thenReturn(caddie);
        when(status.getType()).thenReturn(type);
        return status;
    }

    private ReservationTeam mockReservationTeam(Long id) {
        com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Course course =
                mock(com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Course.class);
        when(course.getId()).thenReturn(1L);
        when(course.getName()).thenReturn("A코스");

        TeeTime teeTime = mock(TeeTime.class);
        when(teeTime.getId()).thenReturn(id);
        when(teeTime.getPlayDate()).thenReturn(DATE);
        when(teeTime.getStartTime()).thenReturn(LocalTime.of(8, 0));
        when(teeTime.getCourse()).thenReturn(course);

        ReservationTeam team = mock(ReservationTeam.class);
        when(team.getId()).thenReturn(id);
        when(team.getTeeTime()).thenReturn(teeTime);
        when(team.getGolfCourse()).thenReturn(golfCourse);
        return team;
    }

    private Assignment mockExistingAssignment(Long id, Caddie caddie, boolean isHalfBack) {
        Assignment a = mock(Assignment.class);
        when(a.getCaddie()).thenReturn(caddie);
        when(a.getReservationTeam()).thenReturn(mock(ReservationTeam.class));
        when(a.getIsDeleted()).thenReturn(false);
        return a;
    }

    private Assignment mockActiveAssignment(Long id, Caddie caddie) {
        ReservationTeam team = mockReservationTeam(id * 10);
        Assignment a = Assignment.create(golfCourse, team, caddie, DATE, false, false);
        ReflectionTestUtils.setField(a, "id", id);
        return a;
    }

    private Assignment mockLockedAssignment(Long id, Caddie caddie) {
        ReservationTeam team = mockReservationTeam(id * 10);
        Assignment a = Assignment.create(golfCourse, team, caddie, DATE, true, false);
        ReflectionTestUtils.setField(a, "id", id);
        return a;
    }

    private Assignment mockConfirmedAssignment(Long id, Caddie caddie) {
        ReservationTeam team = mockReservationTeam(id * 10);
        Assignment a = Assignment.create(golfCourse, team, caddie, DATE, false, false);
        a.confirm();
        ReflectionTestUtils.setField(a, "id", id);
        return a;
    }

    private Assignment mockCompletedAssignment(Long id, Caddie caddie) {
        ReservationTeam team = mockReservationTeam(id * 10);
        Assignment a = Assignment.create(golfCourse, team, caddie, DATE, false, false);
        a.complete();
        ReflectionTestUtils.setField(a, "id", id);
        return a;
    }
}
