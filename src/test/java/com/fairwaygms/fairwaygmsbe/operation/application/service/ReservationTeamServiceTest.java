package com.fairwaygms.fairwaygmsbe.operation.application.service;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieStatus;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Course;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.ChangeTeeTimeReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.CreateReservationTeamReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.SetDesignatedCaddieReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.UpdateVipReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.ReservationTeamRes;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationPeriod;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationSetting;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.ReservationTeam;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.TeeTime;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.ReservationTeamStatus;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationTeamServiceTest {

    @Mock private ReservationTeamRepository reservationTeamRepository;
    @Mock private TeeTimeRepository teeTimeRepository;
    @Mock private CaddieRepository caddieRepository;
    @Mock private GolfCourseRepository golfCourseRepository;
    @Mock private GolfCourseContextResolver contextResolver;

    private ReservationTeamService service;

    @BeforeEach
    void setUp() {
        service = new ReservationTeamService(reservationTeamRepository, teeTimeRepository,
                caddieRepository, golfCourseRepository, contextResolver);
    }

    // ── 예약팀 등록 ─────────────────────────────────────────────────────────────

    @Test
    void createTeam_succeeds() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(gc));
        TeeTime teeTime = teeTime(1L, gc);
        when(teeTimeRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(teeTime));
        when(reservationTeamRepository.save(any())).thenAnswer(inv -> {
            ReservationTeam t = inv.getArgument(0);
            ReflectionTestUtils.setField(t, "id", 1L);
            return t;
        });

        CreateReservationTeamReq req = new CreateReservationTeamReq(1L, "홍팀", "홍길동", 4, "메모");

        // when
        ReservationTeamRes res = service.createTeam(req, auth);

        // then
        assertThat(res.teamId()).isEqualTo(1L);
        assertThat(res.status()).isEqualTo(ReservationTeamStatus.RESERVED);
        verify(reservationTeamRepository).save(any(ReservationTeam.class));
    }

    @Test
    void createTeam_failsWhenTeeTimeNotFound() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(gc));
        when(teeTimeRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.createTeam(
                new CreateReservationTeamReq(99L, "홍팀", "홍길동", 4, null), auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(OperationErrorCode.TEE_TIME_NOT_FOUND));
    }

    @Test
    void createTeam_failsWhenTeeTimeBelongsToOtherGolfCourse() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(gc));
        GolfCourse otherGc = golfCourse(99L);
        TeeTime teeTime = teeTime(1L, otherGc);
        when(teeTimeRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(teeTime));

        // when & then
        assertThatThrownBy(() -> service.createTeam(
                new CreateReservationTeamReq(1L, "홍팀", "홍길동", 4, null), auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    // ── 예약팀 취소 ─────────────────────────────────────────────────────────────

    @Test
    void cancelTeam_succeeds() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        ReservationTeam team = reservationTeam(1L, gc, ReservationTeamStatus.RESERVED);
        when(reservationTeamRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(team));

        // when
        service.cancelTeam(1L, auth);

        // then
        assertThat(team.getStatus()).isEqualTo(ReservationTeamStatus.CANCELLED);
    }

    @Test
    void cancelTeam_failsWhenAlreadyCancelled() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        // 이미 취소된 팀은 재취소 불가
        ReservationTeam team = reservationTeam(1L, gc, ReservationTeamStatus.CANCELLED);
        when(reservationTeamRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(team));

        // when & then
        assertThatThrownBy(() -> service.cancelTeam(1L, auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(OperationErrorCode.INVALID_TEAM_STATUS));
    }

    // ── 노쇼 처리 ──────────────────────────────────────────────────────────────

    @Test
    void noShow_failsWhenTeamAlreadyCompleted() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        ReservationTeam team = reservationTeam(1L, gc, ReservationTeamStatus.COMPLETED);
        when(reservationTeamRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(team));

        // when & then — 완료된 팀은 노쇼 처리 불가
        assertThatThrownBy(() -> service.noShow(1L, auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(OperationErrorCode.INVALID_TEAM_STATUS));
    }

    // ── 티타임 변경 ─────────────────────────────────────────────────────────────

    @Test
    void changeTeeTime_failsWhenNotReserved() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        // 취소된 팀은 티타임 변경 불가
        ReservationTeam team = reservationTeam(1L, gc, ReservationTeamStatus.CANCELLED);
        when(reservationTeamRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(team));

        // when & then
        assertThatThrownBy(() -> service.changeTeeTime(1L, new ChangeTeeTimeReq(2L), auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(OperationErrorCode.INVALID_TEAM_STATUS));
    }

    // ── 지정 캐디 등록 ─────────────────────────────────────────────────────────

    @Test
    void setDesignatedCaddie_succeeds() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        ReservationTeam team = reservationTeam(1L, gc, ReservationTeamStatus.RESERVED);
        when(reservationTeamRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(team));
        Caddie caddie = caddie(5L, gc);
        when(caddieRepository.findByIdAndIsDeletedFalse(5L)).thenReturn(Optional.of(caddie));

        // when
        ReservationTeamRes res = service.setDesignatedCaddie(1L, new SetDesignatedCaddieReq(5L), auth);

        // then
        assertThat(team.getDesignatedCaddie()).isEqualTo(caddie);
        assertThat(res.designatedCaddieId()).isEqualTo(5L);
    }

    @Test
    void setDesignatedCaddie_failsWhenCaddieNotFound() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        ReservationTeam team = reservationTeam(1L, gc, ReservationTeamStatus.RESERVED);
        when(reservationTeamRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(team));
        when(caddieRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.setDesignatedCaddie(1L, new SetDesignatedCaddieReq(99L), auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(OperationErrorCode.CADDIE_NOT_FOUND));
    }

    // ── VIP 처리 ───────────────────────────────────────────────────────────────

    @Test
    void updateVip_succeeds() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        ReservationTeam team = reservationTeam(1L, gc, ReservationTeamStatus.RESERVED);
        when(reservationTeamRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(team));

        // when
        ReservationTeamRes res = service.updateVip(1L, new UpdateVipReq(true, "VIP 고객"), auth);

        // then
        assertThat(team.getIsVip()).isTrue();
        assertThat(res.isVip()).isTrue();
    }

    // ── 픽스처 ─────────────────────────────────────────────────────────────────

    private AuthenticatedUser manager(Long golfCourseId) {
        return new AuthenticatedUser(2L, UserRole.MANAGER, golfCourseId);
    }

    private GolfCourse golfCourse(Long id) {
        GolfCourse gc = GolfCourse.create("테스트CC", "주소", "000-0000-0000");
        ReflectionTestUtils.setField(gc, "id", id);
        return gc;
    }

    private TeeTime teeTime(Long id, GolfCourse gc) {
        Course course = Course.create(gc, "A코스", 18, 1);
        ReflectionTestUtils.setField(course, "id", 1L);
        OperationSetting setting = OperationSetting.create(gc, "2025-06");
        ReflectionTestUtils.setField(setting, "id", 1L);
        OperationPeriod period = OperationPeriod.create(setting, gc, course, 1,
                LocalTime.of(8, 0), LocalTime.of(12, 0), 10);
        ReflectionTestUtils.setField(period, "id", 1L);
        TeeTime tt = TeeTime.create(gc, period, course, LocalDate.of(2025, 6, 1), LocalTime.of(8, 0));
        ReflectionTestUtils.setField(tt, "id", id);
        return tt;
    }

    private ReservationTeam reservationTeam(Long id, GolfCourse gc, ReservationTeamStatus status) {
        TeeTime tt = teeTime(1L, gc);
        ReservationTeam team = ReservationTeam.create(gc, tt, "홍팀", "홍길동", 4, null);
        if (status != ReservationTeamStatus.RESERVED) {
            switch (status) {
                case CANCELLED -> team.cancel();
                case NO_SHOW -> team.noShow();
                case RAIN_CANCELLED -> team.rainCancel();
                case COMPLETED -> team.complete();
            }
        }
        ReflectionTestUtils.setField(team, "id", id);
        return team;
    }

    private Caddie caddie(Long id, GolfCourse gc) {
        Caddie c = Caddie.createOnApproval(gc, null, "김캐디");
        ReflectionTestUtils.setField(c, "id", id);
        ReflectionTestUtils.setField(c, "status", CaddieStatus.ACTIVE);
        return c;
    }
}
