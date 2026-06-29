package com.fairwaygms.fairwaygmsbe.caddie.application.service;

import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.RegisterDailyStatusReq;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieDailyStatus;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.DailyStatusPriority;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.DailyStatusType;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieDailyStatusRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.caddie.exception.CaddieErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyStatusServiceTest {

    @Mock private CaddieRepository caddieRepository;
    @Mock private CaddieDailyStatusRepository dailyStatusRepository;
    @Mock private GolfCourseRepository golfCourseRepository;

    private DailyStatusService dailyStatusService;

    @BeforeEach
    void setUp() {
        dailyStatusService = new DailyStatusService(caddieRepository, dailyStatusRepository, golfCourseRepository);
    }

    // ─── 등록 ─────────────────────────────────────────────────────────

    @Test
    void 휴무_정상_등록() {
        // given
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = caddie(1L, gc);
        AuthenticatedUser manager = manager(10L);

        when(caddieRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(caddie));
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(gc));
        when(dailyStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new RegisterDailyStatusReq(1L, LocalDate.of(2026, 7, 1), DailyStatusType.DAY_OFF, null, null);

        // when
        var result = dailyStatusService.register(req, manager);

        // then
        assertThat(result.type()).isEqualTo(DailyStatusType.DAY_OFF);
        assertThat(result.priority()).isNull();
    }

    @Test
    void 당번_등록_우선순위_없으면_예외() {
        // given — DUTY 타입인데 priority 누락
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = caddie(1L, gc);

        when(caddieRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(caddie));

        var req = new RegisterDailyStatusReq(1L, LocalDate.of(2026, 7, 1), DailyStatusType.DUTY, null, null);

        // when & then
        assertThatThrownBy(() -> dailyStatusService.register(req, manager(10L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CaddieErrorCode.DUTY_PRIORITY_REQUIRED));
    }

    @Test
    void 당번_1당번_정상_등록() {
        // given
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = caddie(1L, gc);

        when(caddieRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(caddie));
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(gc));
        when(dailyStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new RegisterDailyStatusReq(1L, LocalDate.of(2026, 7, 1), DailyStatusType.DUTY, DailyStatusPriority.FIRST, null);

        // when
        var result = dailyStatusService.register(req, manager(10L));

        // then
        assertThat(result.type()).isEqualTo(DailyStatusType.DUTY);
        assertThat(result.priority()).isEqualTo(DailyStatusPriority.FIRST);
    }

    @Test
    void 다른_골프장_캐디_등록시_FORBIDDEN() {
        // given
        GolfCourse gc = golfCourse(99L); // 다른 골프장
        Caddie caddie = caddie(1L, gc);

        when(caddieRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(caddie));

        var req = new RegisterDailyStatusReq(1L, LocalDate.of(2026, 7, 1), DailyStatusType.DAY_OFF, null, null);

        // when & then
        assertThatThrownBy(() -> dailyStatusService.register(req, manager(10L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    // ─── 삭제 ─────────────────────────────────────────────────────────

    @Test
    void 일별_상태_삭제_정상() {
        // given
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = caddie(1L, gc);
        CaddieDailyStatus status = CaddieDailyStatus.create(caddie, gc, LocalDate.of(2026, 7, 1),
                DailyStatusType.DAY_OFF, null, null);

        when(dailyStatusRepository.findByIdAndIsDeletedFalse(5L)).thenReturn(Optional.of(status));

        // when
        dailyStatusService.delete(5L, manager(10L));

        // then
        assertThat(status.getIsDeleted()).isTrue();
    }

    @Test
    void 존재하지_않는_상태_삭제시_예외() {
        // given
        when(dailyStatusRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> dailyStatusService.delete(999L, manager(10L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CaddieErrorCode.DAILY_STATUS_NOT_FOUND));
    }

    // ─── 헬퍼 ──────────────────────────────────────────────────────────

    private AuthenticatedUser manager(Long golfCourseId) {
        return new AuthenticatedUser(10L, UserRole.MANAGER, golfCourseId);
    }

    private GolfCourse golfCourse(Long id) {
        GolfCourse gc = GolfCourse.create("테스트 골프장", "주소", "010-0000-0000");
        ReflectionTestUtils.setField(gc, "id", id);
        return gc;
    }

    private Caddie caddie(Long id, GolfCourse gc) {
        Caddie c = Caddie.createOnApproval(gc, null, "테스트 캐디");
        c.updateInfo("A01", null, null);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
