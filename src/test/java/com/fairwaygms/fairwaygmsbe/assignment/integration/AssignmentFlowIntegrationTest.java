package com.fairwaygms.fairwaygmsbe.assignment.integration;

import com.fairwaygms.fairwaygmsbe.assignment.application.model.req.*;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.AssignmentRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.AutoAssignRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.DailyScheduleRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.service.AssignmentService;
import com.fairwaygms.fairwaygmsbe.assignment.application.service.DailyScheduleService;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.enums.AssignmentStatus;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentHistoryRepository;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentRepository;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.DailyScheduleRepository;
import com.fairwaygms.fairwaygmsbe.assignment.exception.AssignmentErrorCode;
import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieQueue;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieQueueRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Course;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.CourseRepository;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationPeriod;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationSetting;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.ReservationTeam;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.TeeTime;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.OperationPeriodRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.OperationSettingRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.ReservationTeamRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.TeeTimeRepository;
import com.fairwaygms.fairwaygmsbe.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AssignmentFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired AssignmentService assignmentService;
    @Autowired DailyScheduleService dailyScheduleService;
    @Autowired AssignmentRepository assignmentRepository;
    @Autowired AssignmentHistoryRepository historyRepository;
    @Autowired DailyScheduleRepository dailyScheduleRepository;
    @Autowired GolfCourseRepository golfCourseRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired UserRepository userRepository;
    @Autowired CaddieRepository caddieRepository;
    @Autowired CaddieQueueRepository caddieQueueRepository;
    @Autowired OperationSettingRepository settingRepository;
    @Autowired OperationPeriodRepository periodRepository;
    @Autowired TeeTimeRepository teeTimeRepository;
    @Autowired ReservationTeamRepository teamRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private static final LocalDate TEST_DATE = LocalDate.of(2026, 7, 1);

    private GolfCourse golfCourse;
    private Course course;
    private Caddie caddie;
    private OperationPeriod period;
    private AuthenticatedUser managerAuth;

    @BeforeEach
    void setUp() {
        golfCourse = golfCourseRepository.save(GolfCourse.create("테스트CC", "서울시", "02-0000-0000"));
        course = courseRepository.save(Course.create(golfCourse, "A코스", 18, 1));

        // AssignmentService.findUser()가 실제 DB를 조회하므로 매니저 User를 저장한다
        User managerUser = User.createEmailUser(
                "manager@test.com", passwordEncoder.encode("Manager1!"), "테스트매니저",
                null, UserRole.MANAGER, golfCourse.getId());
        managerUser.approve(999L);
        managerUser = userRepository.save(managerUser);
        managerAuth = new AuthenticatedUser(managerUser.getId(), UserRole.MANAGER, golfCourse.getId());

        // 캐디용 User (승인 없이 직접 저장)
        User caddieUser = User.createEmailUser(
                "caddie@test.com", passwordEncoder.encode("Caddie1!"), "테스트캐디",
                null, UserRole.CADDY, golfCourse.getId());
        caddieUser.approve(999L);
        caddieUser = userRepository.save(caddieUser);
        caddie = caddieRepository.save(Caddie.createOnApproval(golfCourse, caddieUser, "홍길동"));

        OperationSetting setting = settingRepository.save(OperationSetting.create(golfCourse, "2026-07"));
        period = periodRepository.save(OperationPeriod.create(setting, golfCourse, course, 1,
                LocalTime.of(7, 0), LocalTime.of(12, 0), 10));
    }

    // ─── 수동 배정 ────────────────────────────────────────────────────────────

    @Test
    void 수동_배정_성공_시_배정과_이력이_DB에_저장된다() {
        // given
        ReservationTeam team = saveTeam(TEST_DATE, LocalTime.of(8, 0));

        // when
        AssignmentRes result = assignmentService.manualPreAssign(
                new ManualPreAssignReq(team.getId(), caddie.getId(), false, false, "테스트 배정"),
                managerAuth);

        // then
        assertThat(result.caddieId()).isEqualTo(caddie.getId());
        assertThat(result.status()).isEqualTo("ASSIGNED");

        List<Assignment> saved = assignmentRepository.findByGolfCourseAndDateWithDetails(
                golfCourse.getId(), TEST_DATE);
        assertThat(saved).hasSize(1);

        assertThat(historyRepository.findAll()).hasSize(1);
    }

    @Test
    void 같은_팀에_중복_배정_시_예외() {
        // given
        ReservationTeam team = saveTeam(TEST_DATE, LocalTime.of(8, 0));
        assignmentService.manualPreAssign(
                new ManualPreAssignReq(team.getId(), caddie.getId(), false, false, null),
                managerAuth);

        // when / then
        assertThatThrownBy(() ->
                assignmentService.manualPreAssign(
                        new ManualPreAssignReq(team.getId(), caddie.getId(), false, false, null),
                        managerAuth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AssignmentErrorCode.ASSIGNMENT_ALREADY_EXISTS));
    }

    @Test
    void 배정_취소_후_소프트삭제되고_같은_팀에_재배정_가능() {
        // given
        ReservationTeam team = saveTeam(TEST_DATE, LocalTime.of(8, 0));
        AssignmentRes first = assignmentService.manualPreAssign(
                new ManualPreAssignReq(team.getId(), caddie.getId(), false, false, null),
                managerAuth);

        // when
        assignmentService.cancelAssignment(first.id(), "취소 사유", managerAuth);

        // then — soft delete 후 재배정 가능
        AssignmentRes second = assignmentService.manualPreAssign(
                new ManualPreAssignReq(team.getId(), caddie.getId(), false, false, null),
                managerAuth);
        assertThat(second).isNotNull();

        // 취소된 배정은 isDeleted=true이므로 활성 배정 조회에서 제외
        List<Assignment> active = assignmentRepository.findByGolfCourseAndDateWithDetails(
                golfCourse.getId(), TEST_DATE);
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getIsDeleted()).isFalse();
    }

    // ─── 배정표 확정 흐름 ──────────────────────────────────────────────────────

    @Test
    void 배정표_확정_시_ASSIGNED_상태_배정이_모두_CONFIRMED로_변경된다() {
        // given
        ReservationTeam team = saveTeam(TEST_DATE, LocalTime.of(8, 0));
        assignmentService.manualPreAssign(
                new ManualPreAssignReq(team.getId(), caddie.getId(), false, false, null),
                managerAuth);

        DailyScheduleRes schedule = dailyScheduleService.createDailySchedule(
                new CreateDailyScheduleReq(TEST_DATE), managerAuth);

        // when
        DailyScheduleRes confirmed = dailyScheduleService.confirmSchedule(schedule.id(), managerAuth);

        // then
        assertThat(confirmed.status()).isEqualTo("CONFIRMED");

        List<Assignment> assignments = assignmentRepository.findByGolfCourseAndDateWithDetails(
                golfCourse.getId(), TEST_DATE);
        assertThat(assignments).allMatch(a -> a.getStatus() == AssignmentStatus.CONFIRMED);
    }

    @Test
    void 배정표_확정_취소_시_CONFIRMED_배정이_ASSIGNED로_복귀() {
        // given
        ReservationTeam team = saveTeam(TEST_DATE, LocalTime.of(8, 0));
        assignmentService.manualPreAssign(
                new ManualPreAssignReq(team.getId(), caddie.getId(), false, false, null),
                managerAuth);

        DailyScheduleRes schedule = dailyScheduleService.createDailySchedule(
                new CreateDailyScheduleReq(TEST_DATE), managerAuth);
        dailyScheduleService.confirmSchedule(schedule.id(), managerAuth);

        // when
        DailyScheduleRes cancelled = dailyScheduleService.cancelConfirmSchedule(schedule.id(), managerAuth);

        // then
        assertThat(cancelled.status()).isEqualTo("DRAFT");

        List<Assignment> assignments = assignmentRepository.findByGolfCourseAndDateWithDetails(
                golfCourse.getId(), TEST_DATE);
        assertThat(assignments).allMatch(a -> a.getStatus() == AssignmentStatus.ASSIGNED);
    }

    @Test
    void 같은_날짜_배정표_중복_생성_거부() {
        // given
        dailyScheduleService.createDailySchedule(new CreateDailyScheduleReq(TEST_DATE), managerAuth);

        // when / then
        assertThatThrownBy(() ->
                dailyScheduleService.createDailySchedule(new CreateDailyScheduleReq(TEST_DATE), managerAuth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AssignmentErrorCode.DAILY_SCHEDULE_ALREADY_EXISTS));
    }

    @Test
    void 배정표_완료_처리_시_모든_배정이_COMPLETED로_변경된다() {
        // given
        ReservationTeam team = saveTeam(TEST_DATE, LocalTime.of(8, 0));
        assignmentService.manualPreAssign(
                new ManualPreAssignReq(team.getId(), caddie.getId(), false, false, null),
                managerAuth);

        DailyScheduleRes schedule = dailyScheduleService.createDailySchedule(
                new CreateDailyScheduleReq(TEST_DATE), managerAuth);
        dailyScheduleService.confirmSchedule(schedule.id(), managerAuth);

        // when
        DailyScheduleRes completed = dailyScheduleService.completeDailySchedule(schedule.id(), managerAuth);

        // then
        assertThat(completed.status()).isEqualTo("COMPLETED");

        List<Assignment> assignments = assignmentRepository.findByGolfCourseAndDateWithDetails(
                golfCourse.getId(), TEST_DATE);
        assertThat(assignments).allMatch(a -> a.getStatus() == AssignmentStatus.COMPLETED);
    }

    // ─── 자동배정 ──────────────────────────────────────────────────────────────

    @Test
    void 자동배정_큐_순서대로_팀에_배정된다() {
        // given
        ReservationTeam team = saveTeam(TEST_DATE, LocalTime.of(8, 0));
        caddieQueueRepository.save(CaddieQueue.create(caddie, golfCourse, TEST_DATE, 1));

        // when
        AutoAssignRes result = assignmentService.autoAssign(
                new AutoAssignReq(TEST_DATE, period.getId(), null), managerAuth);

        // then
        assertThat(result.assignedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(0);

        List<Assignment> assignments = assignmentRepository.findByGolfCourseAndDateWithDetails(
                golfCourse.getId(), TEST_DATE);
        assertThat(assignments).hasSize(1);
        assertThat(assignments.get(0).getCaddie().getId()).isEqualTo(caddie.getId());
    }

    @Test
    void 자동배정_이미_배정된_팀은_스킵한다() {
        // given — 미리 수동 배정
        ReservationTeam team1 = saveTeam(TEST_DATE, LocalTime.of(8, 0));
        ReservationTeam team2 = saveTeam(TEST_DATE, LocalTime.of(8, 10));
        assignmentService.manualPreAssign(
                new ManualPreAssignReq(team1.getId(), caddie.getId(), false, false, null),
                managerAuth);
        caddieQueueRepository.save(CaddieQueue.create(caddie, golfCourse, TEST_DATE, 1));

        // when
        AutoAssignRes result = assignmentService.autoAssign(
                new AutoAssignReq(TEST_DATE, period.getId(), null), managerAuth);

        // then — team1은 이미 배정, team2만 새로 배정
        assertThat(result.assignedCount()).isEqualTo(1);

        List<Assignment> assignments = assignmentRepository.findByGolfCourseAndDateWithDetails(
                golfCourse.getId(), TEST_DATE);
        assertThat(assignments).hasSize(2);
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────────────

    private ReservationTeam saveTeam(LocalDate playDate, LocalTime startTime) {
        TeeTime teeTime = teeTimeRepository.save(
                TeeTime.create(golfCourse, period, course, playDate, startTime));
        return teamRepository.save(
                ReservationTeam.create(golfCourse, teeTime, "홍길동팀", "홍길동", 4, null));
    }
}
