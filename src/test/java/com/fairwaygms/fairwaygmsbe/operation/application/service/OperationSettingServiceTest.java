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
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.CreateOperationSettingReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.PeriodReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.UpdateOperationSettingReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.UpdatePeriodReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.OperationSettingRes;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationPeriod;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationSetting;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.OperationPeriodRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.OperationSettingRepository;
import com.fairwaygms.fairwaygmsbe.operation.exception.OperationErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationSettingServiceTest {

    @Mock private OperationSettingRepository settingRepository;
    @Mock private OperationPeriodRepository periodRepository;
    @Mock private GolfCourseRepository golfCourseRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private GolfCourseContextResolver contextResolver;

    private OperationSettingService service;

    @BeforeEach
    void setUp() {
        service = new OperationSettingService(settingRepository, periodRepository,
                golfCourseRepository, courseRepository, contextResolver);
    }

    // ── 운영 설정 등록 ─────────────────────────────────────────────────────────

    @Test
    void createSetting_managerSucceeds() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        when(settingRepository.existsByGolfCourse_IdAndYearMonthAndIsDeletedFalse(10L, "2025-06")).thenReturn(false);
        GolfCourse gc = golfCourse(10L);
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(gc));
        Course course = course(1L, gc);
        when(courseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(course));
        when(settingRepository.save(any())).thenAnswer(inv -> {
            OperationSetting s = inv.getArgument(0);
            ReflectionTestUtils.setField(s, "id", 1L);
            return s;
        });
        when(periodRepository.save(any())).thenAnswer(inv -> {
            OperationPeriod p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 1L);
            return p;
        });

        PeriodReq periodReq = new PeriodReq(1L, 1, LocalTime.of(8, 0), LocalTime.of(12, 0), 10);
        CreateOperationSettingReq req = new CreateOperationSettingReq("2025-06", List.of(periodReq));

        // when
        OperationSettingRes res = service.createSetting(req, auth);

        // then
        assertThat(res.yearMonth()).isEqualTo("2025-06");
        assertThat(res.periods()).hasSize(1);
        verify(settingRepository).save(any(OperationSetting.class));
        verify(periodRepository).save(any(OperationPeriod.class));
    }

    @Test
    void createSetting_failsWhenNotManager() {
        // given
        AuthenticatedUser auth = admin();

        // when & then — ADMIN은 Manager 전용 운영 설정 API에 접근 불가
        assertThatThrownBy(() -> service.createSetting(
                new CreateOperationSettingReq("2025-06", List.of()), auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void createSetting_failsOnDuplicateYearMonth() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        when(settingRepository.existsByGolfCourse_IdAndYearMonthAndIsDeletedFalse(10L, "2025-06")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> service.createSetting(
                new CreateOperationSettingReq("2025-06", List.of()), auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(OperationErrorCode.SETTING_ALREADY_EXISTS));
    }

    // ── 운영 설정 조회 ─────────────────────────────────────────────────────────

    @Test
    void getSetting_managerSucceeds() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        OperationSetting setting = operationSetting(1L, gc, "2025-06");
        when(settingRepository.findByGolfCourse_IdAndYearMonthAndIsDeletedFalse(10L, "2025-06"))
                .thenReturn(Optional.of(setting));
        when(periodRepository.findByOperationSetting_IdAndIsDeletedFalse(1L)).thenReturn(List.of());

        // when
        OperationSettingRes res = service.getSetting("2025-06", auth);

        // then
        assertThat(res.yearMonth()).isEqualTo("2025-06");
    }

    @Test
    void getSetting_failsWhenNotFound() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        when(settingRepository.findByGolfCourse_IdAndYearMonthAndIsDeletedFalse(10L, "2025-06"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.getSetting("2025-06", auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(OperationErrorCode.SETTING_NOT_FOUND));
    }

    // ── 운영 설정 수정 ─────────────────────────────────────────────────────────

    @Test
    void updateSetting_succeeds() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        OperationSetting setting = operationSetting(1L, gc, "2025-06");
        when(settingRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(setting));
        OperationPeriod period = operationPeriod(1L, setting, course(1L, gc));
        when(periodRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(period));
        when(periodRepository.findByOperationSetting_IdAndIsDeletedFalse(1L)).thenReturn(List.of(period));

        UpdatePeriodReq periodReq = new UpdatePeriodReq(1L, LocalTime.of(9, 0), LocalTime.of(13, 0), 15, true);
        UpdateOperationSettingReq req = new UpdateOperationSettingReq(List.of(periodReq));

        // when
        OperationSettingRes res = service.updateSetting(1L, req, auth);

        // then
        assertThat(res.yearMonth()).isEqualTo("2025-06");
        assertThat(period.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(period.getTeeTimeInterval()).isEqualTo(15);
    }

    @Test
    void updateSetting_failsWhenSettingBelongsToOtherGolfCourse() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse otherGc = golfCourse(99L);
        OperationSetting otherSetting = operationSetting(1L, otherGc, "2025-06");
        when(settingRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(otherSetting));

        // when & then — 다른 골프장의 설정에 접근 시 FORBIDDEN
        assertThatThrownBy(() -> service.updateSetting(1L,
                new UpdateOperationSettingReq(List.of()), auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void updateSetting_failsWhenPeriodNotFound() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        OperationSetting setting = operationSetting(1L, gc, "2025-06");
        when(settingRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(setting));
        when(periodRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        UpdatePeriodReq periodReq = new UpdatePeriodReq(99L, LocalTime.of(9, 0), LocalTime.of(13, 0), 15, true);

        // when & then
        assertThatThrownBy(() -> service.updateSetting(1L,
                new UpdateOperationSettingReq(List.of(periodReq)), auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(OperationErrorCode.PERIOD_NOT_FOUND));
    }

    // ── 픽스처 ─────────────────────────────────────────────────────────────────

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

    private OperationPeriod operationPeriod(Long id, OperationSetting setting, Course course) {
        OperationPeriod p = OperationPeriod.create(setting, setting.getGolfCourse(), course,
                1, LocalTime.of(8, 0), LocalTime.of(12, 0), 10);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }
}
