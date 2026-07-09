package com.fairwaygms.fairwaygmsbe.operation.application.service;

import com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Course;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.CourseRepository;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.CreateTeeTimeReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.GenerateTeeTimesReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.GenerateTeeTimesRes;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.TeeTimeRes;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationPeriod;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationSetting;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.TeeTime;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.TeeTimeStatus;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.OperationPeriodRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.OperationSettingRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.ReservationTeamRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.TeeTimeRepository;
import com.fairwaygms.fairwaygmsbe.operation.exception.OperationErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeeTimeServiceTest {

    @Mock private TeeTimeRepository teeTimeRepository;
    @Mock private OperationSettingRepository settingRepository;
    @Mock private OperationPeriodRepository periodRepository;
    @Mock private ReservationTeamRepository reservationTeamRepository;
    @Mock private GolfCourseRepository golfCourseRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private GolfCourseContextResolver contextResolver;

    private TeeTimeService service;

    @BeforeEach
    void setUp() {
        service = new TeeTimeService(teeTimeRepository, settingRepository, periodRepository,
                reservationTeamRepository, golfCourseRepository, courseRepository, contextResolver);
    }

    // ── 티타임 자동 생성 ────────────────────────────────────────────────────────

    @Test
    void generateTeeTimes_createsCorrectSlotCount() {
        // given — 2025-06 (30일), 부 1개: 08:00~08:20 10분 간격 → 슬롯 3개/일 → 총 90개
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(gc));
        OperationSetting setting = operationSetting(1L, gc, "2025-06");
        when(settingRepository.findByGolfCourse_IdAndYearMonthAndIsDeletedFalse(10L, "2025-06"))
                .thenReturn(Optional.of(setting));
        Course course = course(1L, gc);
        OperationPeriod period = operationPeriod(1L, setting, course,
                LocalTime.of(8, 0), LocalTime.of(8, 20), 10);
        when(periodRepository.findByOperationSetting_IdAndIsActiveTrueAndIsDeletedFalse(1L))
                .thenReturn(List.of(period));
        when(teeTimeRepository.existsByGolfCourse_IdAndCourse_IdAndPlayDateAndStartTimeAndIsDeletedFalse(
                any(), any(), any(), any())).thenReturn(false);
        when(teeTimeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        GenerateTeeTimesRes res = service.generateTeeTimes(
                new GenerateTeeTimesReq("2025-06", null), auth);

        // then — 30일 × 3슬롯 = 90
        assertThat(res.generatedCount()).isEqualTo(90);
        assertThat(res.yearMonth()).isEqualTo("2025-06");
        verify(teeTimeRepository, times(90)).save(any(TeeTime.class));
    }

    @Test
    void generateTeeTimes_skipsExistingSlots() {
        // given — 08:00 슬롯만 이미 존재, 08:10은 신규 생성
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(gc));
        OperationSetting setting = operationSetting(1L, gc, "2025-06");
        when(settingRepository.findByGolfCourse_IdAndYearMonthAndIsDeletedFalse(10L, "2025-06"))
                .thenReturn(Optional.of(setting));
        Course course = course(1L, gc);
        OperationPeriod period = operationPeriod(1L, setting, course,
                LocalTime.of(8, 0), LocalTime.of(8, 10), 10);
        when(periodRepository.findByOperationSetting_IdAndIsActiveTrueAndIsDeletedFalse(1L))
                .thenReturn(List.of(period));
        // 08:00은 이미 존재 → exists=true / 08:10은 신규 → exists=false
        when(teeTimeRepository.existsByGolfCourse_IdAndCourse_IdAndPlayDateAndStartTimeAndIsDeletedFalse(
                eq(10L), eq(1L), any(), eq(LocalTime.of(8, 0)))).thenReturn(true);
        when(teeTimeRepository.existsByGolfCourse_IdAndCourse_IdAndPlayDateAndStartTimeAndIsDeletedFalse(
                eq(10L), eq(1L), any(), eq(LocalTime.of(8, 10)))).thenReturn(false);
        when(teeTimeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        GenerateTeeTimesRes res = service.generateTeeTimes(
                new GenerateTeeTimesReq("2025-06", null), auth);

        // then — 30일 × 1슬롯(08:00 스킵, 08:10만 생성) = 30
        assertThat(res.generatedCount()).isEqualTo(30);
    }

    @Test
    void generateTeeTimes_failsWhenSettingNotFound() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(gc));
        when(settingRepository.findByGolfCourse_IdAndYearMonthAndIsDeletedFalse(10L, "2025-06"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.generateTeeTimes(
                new GenerateTeeTimesReq("2025-06", null), auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(OperationErrorCode.SETTING_NOT_FOUND));
    }

    // ── 티타임 수동 추가 ────────────────────────────────────────────────────────

    @Test
    void addTeeTime_failsOnDuplicate() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(gc));
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course(1L, gc)));
        when(teeTimeRepository.existsByGolfCourse_IdAndCourse_IdAndPlayDateAndStartTimeAndIsDeletedFalse(
                10L, 1L, LocalDate.of(2025, 6, 1), LocalTime.of(8, 0))).thenReturn(true);

        CreateTeeTimeReq req = new CreateTeeTimeReq(1L, LocalDate.of(2025, 6, 1), LocalTime.of(8, 0), 1);

        // when & then
        assertThatThrownBy(() -> service.addTeeTime(req, auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(OperationErrorCode.DUPLICATE_TEE_TIME));
    }

    @Test
    void addTeeTime_failsWhenCaddy() {
        // when & then
        assertThatThrownBy(() -> service.addTeeTime(
                new CreateTeeTimeReq(1L, LocalDate.of(2025, 6, 1), LocalTime.of(8, 0), 1), caddy()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    // ── 티타임 마감 ─────────────────────────────────────────────────────────────

    @Test
    void closeTeeTime_succeeds() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        TeeTime teeTime = teeTime(1L, gc);
        when(teeTimeRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(teeTime));

        // when
        service.closeTeeTime(1L, auth);

        // then
        assertThat(teeTime.getStatus()).isEqualTo(TeeTimeStatus.CLOSED);
    }

    @Test
    void closeTeeTime_failsForOtherGolfCourse() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse otherGc = golfCourse(99L);
        TeeTime teeTime = teeTime(1L, otherGc);
        when(teeTimeRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(teeTime));

        // when & then
        assertThatThrownBy(() -> service.closeTeeTime(1L, auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void closeTeeTime_failsWhenNotFound() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        when(teeTimeRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.closeTeeTime(99L, auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(OperationErrorCode.TEE_TIME_NOT_FOUND));
    }

    // ── 픽스처 ─────────────────────────────────────────────────────────────────

    private AuthenticatedUser caddy() {
        return new AuthenticatedUser(2L, UserRole.CADDY, 10L);
    }

    private AuthenticatedUser admin() {
        return new AuthenticatedUser(1L, UserRole.ADMIN, null);
    }

    private AuthenticatedUser manager(Long golfCourseId) {
        return new AuthenticatedUser(2L, UserRole.MANAGER, golfCourseId);
    }

    private GolfCourse golfCourse(Long id) {
        GolfCourse gc = GolfCourse.create("테스트CC", "주소", "000-0000-0000");
        ReflectionTestUtils.setField(gc, "id", id);
        return gc;
    }

    private Course course(Long id, GolfCourse gc) {
        Course c = Course.create(gc, "A코스", 18, 1);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private OperationSetting operationSetting(Long id, GolfCourse gc, String yearMonth) {
        OperationSetting s = OperationSetting.create(gc, yearMonth);
        ReflectionTestUtils.setField(s, "id", id);
        return s;
    }

    private OperationPeriod operationPeriod(Long id, OperationSetting setting, Course course,
                                             LocalTime start, LocalTime end, int interval) {
        OperationPeriod p = OperationPeriod.create(setting, setting.getGolfCourse(), course,
                1, start, end, interval);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    private TeeTime teeTime(Long id, GolfCourse gc) {
        Course course = course(1L, gc);
        OperationSetting setting = operationSetting(1L, gc, "2025-06");
        OperationPeriod period = operationPeriod(1L, setting, course,
                LocalTime.of(8, 0), LocalTime.of(12, 0), 10);
        TeeTime tt = TeeTime.create(gc, period, course, LocalDate.of(2025, 6, 1), LocalTime.of(8, 0));
        ReflectionTestUtils.setField(tt, "id", id);
        return tt;
    }
}
