package com.fairwaygms.fairwaygmsbe.operation.integration;

import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Course;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.CourseRepository;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.*;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.*;
import com.fairwaygms.fairwaygmsbe.operation.application.service.*;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.RainCancellationPolicyType;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.ReservationTeamStatus;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.TeeTimeStatus;
import com.fairwaygms.fairwaygmsbe.operation.exception.OperationErrorCode;
import com.fairwaygms.fairwaygmsbe.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired OperationSettingService settingService;
    @Autowired OperationPolicyService policyService;
    @Autowired TeeTimeService teeTimeService;
    @Autowired ReservationTeamService teamService;
    @Autowired GolfCourseRepository golfCourseRepository;
    @Autowired CourseRepository courseRepository;

    private GolfCourse golfCourse;
    private Course course;
    private AuthenticatedUser managerAuth;

    @BeforeEach
    void setUp() {
        golfCourse = golfCourseRepository.save(GolfCourse.create("테스트CC", "서울시", "02-0000-0000"));
        course = courseRepository.save(Course.create(golfCourse, "A코스", 18, 1));
        managerAuth = new AuthenticatedUser(1L, UserRole.MANAGER, golfCourse.getId());
    }

    // ─── 운영 설정 흐름 ───────────────────────────────────────────────────────

    @Test
    void 운영_설정_등록_후_yearMonth로_조회() {
        // given
        OperationSettingRes created = createSetting("2025-06");

        // when
        OperationSettingRes found = settingService.getSetting("2025-06", managerAuth);

        // then
        assertThat(found.settingId()).isEqualTo(created.settingId());
        assertThat(found.yearMonth()).isEqualTo("2025-06");
        assertThat(found.periods()).hasSize(1);
    }

    @Test
    void 같은_월_운영_설정_중복_등록_거부() {
        // given
        createSetting("2025-06");

        // when & then
        assertThatThrownBy(() -> createSetting("2025-06"))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(OperationErrorCode.SETTING_ALREADY_EXISTS));
    }

    @Test
    void 운영_설정_수정_후_부_시간_반영() {
        // given
        OperationSettingRes setting = createSetting("2025-06");
        Long periodId = setting.periods().get(0).periodId();

        UpdatePeriodReq periodReq = new UpdatePeriodReq(periodId, LocalTime.of(9, 0), LocalTime.of(17, 0), 15, true);
        UpdateOperationSettingReq updateReq = new UpdateOperationSettingReq(List.of(periodReq));

        // when
        OperationSettingRes updated = settingService.updateSetting(setting.settingId(), updateReq, managerAuth);

        // then
        assertThat(updated.periods().get(0).startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(updated.periods().get(0).teeTimeInterval()).isEqualTo(15);
    }

    // ─── 티타임 자동 생성 흐름 ────────────────────────────────────────────────

    @Test
    void 티타임_자동_생성_후_날짜_기준_조회() {
        // given — 2025-06, 08:00~08:10 10분 간격 (슬롯 2개/일 × 30일 = 60개)
        createSetting("2025-06");

        // when
        GenerateTeeTimesRes result = teeTimeService.generateTeeTimes(
                new GenerateTeeTimesReq("2025-06", null), managerAuth);

        // then
        assertThat(result.generatedCount()).isEqualTo(60);

        List<TeeTimeRes> teeTimes = teeTimeService.listTeeTimes(
                LocalDate.of(2025, 6, 1), null, null, managerAuth);
        assertThat(teeTimes).hasSize(2);
        assertThat(teeTimes.get(0).startTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(teeTimes.get(1).startTime()).isEqualTo(LocalTime.of(8, 10));
    }

    @Test
    void 티타임_자동_생성_중복_실행_시_기존_슬롯_스킵() {
        // given
        createSetting("2025-06");
        GenerateTeeTimesRes first = teeTimeService.generateTeeTimes(
                new GenerateTeeTimesReq("2025-06", null), managerAuth);

        // when — 재실행
        GenerateTeeTimesRes second = teeTimeService.generateTeeTimes(
                new GenerateTeeTimesReq("2025-06", null), managerAuth);

        // then — 2차 생성 시 모두 스킵
        assertThat(first.generatedCount()).isEqualTo(60);
        assertThat(second.generatedCount()).isEqualTo(0);
    }

    @Test
    void 티타임_수동_추가() {
        // given
        createSetting("2025-06");

        CreateTeeTimeReq req = new CreateTeeTimeReq(
                course.getId(), LocalDate.of(2025, 6, 1), LocalTime.of(7, 30), 1);

        // when
        TeeTimeRes res = teeTimeService.addTeeTime(req, managerAuth);

        // then
        assertThat(res.startTime()).isEqualTo(LocalTime.of(7, 30));
        assertThat(res.status()).isEqualTo(TeeTimeStatus.OPEN);
    }

    @Test
    void 티타임_수동_추가_중복_시_거부() {
        // given — 자동 생성으로 08:00 슬롯 존재
        createSetting("2025-06");
        teeTimeService.generateTeeTimes(new GenerateTeeTimesReq("2025-06", null), managerAuth);

        CreateTeeTimeReq req = new CreateTeeTimeReq(
                course.getId(), LocalDate.of(2025, 6, 1), LocalTime.of(8, 0), 1);

        // when & then
        assertThatThrownBy(() -> teeTimeService.addTeeTime(req, managerAuth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(OperationErrorCode.DUPLICATE_TEE_TIME));
    }

    @Test
    void 티타임_마감_처리() {
        // given
        createSetting("2025-06");
        teeTimeService.generateTeeTimes(new GenerateTeeTimesReq("2025-06", null), managerAuth);
        TeeTimeRes teeTime = teeTimeService.listTeeTimes(
                LocalDate.of(2025, 6, 1), null, null, managerAuth).get(0);

        // when
        teeTimeService.closeTeeTime(teeTime.teeTimeId(), managerAuth);

        // then
        List<TeeTimeRes> teeTimes = teeTimeService.listTeeTimes(
                LocalDate.of(2025, 6, 1), null, null, managerAuth);
        TeeTimeRes closed = teeTimes.stream()
                .filter(t -> t.teeTimeId().equals(teeTime.teeTimeId()))
                .findFirst().orElseThrow();
        assertThat(closed.status()).isEqualTo(TeeTimeStatus.CLOSED);
    }

    // ─── 예약팀 흐름 ─────────────────────────────────────────────────────────

    @Test
    void 예약팀_등록_후_날짜_기준_목록_조회() {
        // given
        createSetting("2025-06");
        teeTimeService.generateTeeTimes(new GenerateTeeTimesReq("2025-06", null), managerAuth);
        TeeTimeRes teeTime = teeTimeService.listTeeTimes(
                LocalDate.of(2025, 6, 1), null, null, managerAuth).get(0);

        CreateReservationTeamReq req = new CreateReservationTeamReq(
                teeTime.teeTimeId(), "홍팀", "홍길동", 4, "메모");

        // when
        ReservationTeamRes created = teamService.createTeam(req, managerAuth);

        // then
        assertThat(created.status()).isEqualTo(ReservationTeamStatus.RESERVED);

        List<ReservationTeamRes> teams = teamService.listTeams(
                LocalDate.of(2025, 6, 1), null, null, managerAuth);
        assertThat(teams).hasSize(1);
        assertThat(teams.get(0).teamId()).isEqualTo(created.teamId());
    }

    @Test
    void 예약팀_취소_상태_전이() {
        // given
        Long teamId = createTeamForDate(LocalDate.of(2025, 6, 1));

        // when
        teamService.cancelTeam(teamId, managerAuth);

        // then
        ReservationTeamDetailRes detail = teamService.getTeam(teamId, managerAuth);
        assertThat(detail.status()).isEqualTo(ReservationTeamStatus.CANCELLED);
    }

    @Test
    void 이미_취소된_팀_재취소_거부() {
        // given
        Long teamId = createTeamForDate(LocalDate.of(2025, 6, 1));
        teamService.cancelTeam(teamId, managerAuth);

        // when & then
        assertThatThrownBy(() -> teamService.cancelTeam(teamId, managerAuth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(OperationErrorCode.INVALID_TEAM_STATUS));
    }

    @Test
    void 예약팀_노쇼_처리() {
        // given
        Long teamId = createTeamForDate(LocalDate.of(2025, 6, 1));

        // when
        teamService.noShow(teamId, managerAuth);

        // then
        assertThat(teamService.getTeam(teamId, managerAuth).status())
                .isEqualTo(ReservationTeamStatus.NO_SHOW);
    }

    @Test
    void 예약팀_티타임_변경() {
        // given — 두 티타임이 있을 때 팀을 다른 티타임으로 이동
        createSetting("2025-06");
        teeTimeService.generateTeeTimes(new GenerateTeeTimesReq("2025-06", null), managerAuth);
        List<TeeTimeRes> teeTimes = teeTimeService.listTeeTimes(
                LocalDate.of(2025, 6, 1), null, null, managerAuth);

        Long originalTeeTimeId = teeTimes.get(0).teeTimeId();
        Long newTeeTimeId = teeTimes.get(1).teeTimeId();

        ReservationTeamRes team = teamService.createTeam(
                new CreateReservationTeamReq(originalTeeTimeId, "홍팀", "홍길동", 4, null), managerAuth);

        // when
        teamService.changeTeeTime(team.teamId(), new ChangeTeeTimeReq(newTeeTimeId), managerAuth);

        // then
        ReservationTeamDetailRes detail = teamService.getTeam(team.teamId(), managerAuth);
        assertThat(detail.teeTimeId()).isEqualTo(newTeeTimeId);
    }

    // ─── 정책 흐름 ───────────────────────────────────────────────────────────

    @Test
    void 특별_운영일_등록_및_중복_거부() {
        // given
        LocalDate specialDate = LocalDate.of(2025, 6, 6);
        policyService.createSpecialDay(new CreateSpecialDayReq(specialDate, "현충일"), managerAuth);

        // when & then
        assertThatThrownBy(() ->
                policyService.createSpecialDay(new CreateSpecialDayReq(specialDate, "중복"), managerAuth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(OperationErrorCode.SPECIAL_DAY_ALREADY_EXISTS));
    }

    @Test
    void 우천취소_정책_최초_생성_후_수정() {
        // given — 최초 생성
        RainPolicyRes created = policyService.upsertRainPolicy(
                new UpdateRainPolicyReq(RainCancellationPolicyType.KEEP_ORDER), managerAuth);
        assertThat(created.policyType()).isEqualTo(RainCancellationPolicyType.KEEP_ORDER);

        // when — 수정
        RainPolicyRes updated = policyService.upsertRainPolicy(
                new UpdateRainPolicyReq(RainCancellationPolicyType.RESEQUENCE), managerAuth);

        // then — ID가 같고 타입만 변경
        assertThat(updated.policyId()).isEqualTo(created.policyId());
        assertThat(updated.policyType()).isEqualTo(RainCancellationPolicyType.RESEQUENCE);
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────

    private OperationSettingRes createSetting(String yearMonth) {
        PeriodReq period = new PeriodReq(course.getId(), 1, LocalTime.of(8, 0), LocalTime.of(8, 10), 10);
        return settingService.createSetting(
                new CreateOperationSettingReq(yearMonth, List.of(period)), managerAuth);
    }

    private Long createTeamForDate(LocalDate date) {
        createSetting(date.getYear() + "-" + String.format("%02d", date.getMonthValue()));
        teeTimeService.generateTeeTimes(
                new GenerateTeeTimesReq(date.getYear() + "-" + String.format("%02d", date.getMonthValue()), null),
                managerAuth);
        TeeTimeRes teeTime = teeTimeService.listTeeTimes(date, null, null, managerAuth).get(0);
        ReservationTeamRes team = teamService.createTeam(
                new CreateReservationTeamReq(teeTime.teeTimeId(), "홍팀", "홍길동", 4, null), managerAuth);
        return team.teamId();
    }
}
