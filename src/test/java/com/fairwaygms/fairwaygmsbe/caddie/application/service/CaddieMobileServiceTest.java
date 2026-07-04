package com.fairwaygms.fairwaygmsbe.caddie.application.service;

import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieQueue;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieWorkPattern;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieQueueRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieWorkPatternRepository;
import com.fairwaygms.fairwaygmsbe.caddie.exception.CaddieErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaddieMobileServiceTest {

    @Mock private CaddieRepository caddieRepository;
    @Mock private CaddieWorkPatternRepository workPatternRepository;
    @Mock private CaddieQueueRepository queueRepository;
    @Mock private com.fairwaygms.fairwaygmsbe.operation.domain.repository.OperationSettingRepository operationSettingRepository;
    @Mock private com.fairwaygms.fairwaygmsbe.operation.domain.repository.OperationPeriodRepository operationPeriodRepository;

    private CaddieMobileService caddieMobileService;

    @BeforeEach
    void setUp() {
        caddieMobileService = new CaddieMobileService(caddieRepository, workPatternRepository, queueRepository,
                operationSettingRepository, operationPeriodRepository);
    }

    // ─── 기본정보 조회 ─────────────────────────────────────────────────

    @Test
    void 내_기본정보_근무패턴_포함_정상_조회() {
        // given
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = caddie(1L, gc, "A01");
        CaddieWorkPattern pattern = CaddieWorkPattern.createDefault(caddie, gc);

        when(caddieRepository.findByUser_IdAndIsDeletedFalse(20L)).thenReturn(Optional.of(caddie));
        when(workPatternRepository.findByCaddie_IdAndIsDeletedFalse(1L)).thenReturn(Optional.of(pattern));

        // when
        var result = caddieMobileService.getMyInfo(caddy(20L, 10L));

        // then
        assertThat(result.caddieId()).isEqualTo(1L);
        assertThat(result.caddieNumber()).isEqualTo("A01");
        assertThat(result.workPattern()).isNotNull();
        assertThat(result.workPattern().canWeekday()).isTrue();
    }

    @Test
    void 내_기본정보_근무패턴_없어도_정상_응답() {
        // given — 승인 직후 workPattern 미생성 상태
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = caddie(1L, gc, "A01");

        when(caddieRepository.findByUser_IdAndIsDeletedFalse(20L)).thenReturn(Optional.of(caddie));
        when(workPatternRepository.findByCaddie_IdAndIsDeletedFalse(1L)).thenReturn(Optional.empty());

        // when
        var result = caddieMobileService.getMyInfo(caddy(20L, 10L));

        // then
        assertThat(result.workPattern()).isNull();
    }

    @Test
    void MANAGER가_내_기본정보_조회시_FORBIDDEN() {
        // CADDY 전용 API를 MANAGER가 호출하면 FORBIDDEN
        assertThatThrownBy(() -> caddieMobileService.getMyInfo(manager(10L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void userId_연동된_캐디_없으면_예외() {
        // given — user 계정은 있지만 caddie 레코드가 없는 경우
        when(caddieRepository.findByUser_IdAndIsDeletedFalse(20L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> caddieMobileService.getMyInfo(caddy(20L, 10L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CaddieErrorCode.CADDIE_NOT_FOUND));
    }

    // ─── 대기 순번 조회 ────────────────────────────────────────────────

    @Test
    void 내_대기_순번_정상_조회() {
        // given
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = caddie(1L, gc, "A01");
        LocalDate date = LocalDate.of(2026, 7, 1);
        CaddieQueue queue = CaddieQueue.create(caddie, gc, date, 3);

        when(caddieRepository.findByUser_IdAndIsDeletedFalse(20L)).thenReturn(Optional.of(caddie));
        when(queueRepository.findByCaddie_IdAndQueueDateAndIsDeletedFalse(1L, date))
                .thenReturn(Optional.of(queue));

        // when
        var result = caddieMobileService.getMyQueue(caddy(20L, 10L), date);

        // then
        assertThat(result.queueNumber()).isEqualTo(3);
        assertThat(result.queueDate()).isEqualTo(date);
    }

    @Test
    void 순번_미등록이면_queueNumber_null_반환() {
        // given — 해당 날짜에 순번이 아직 없는 상태
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = caddie(1L, gc, "A01");
        LocalDate date = LocalDate.of(2026, 7, 1);

        when(caddieRepository.findByUser_IdAndIsDeletedFalse(20L)).thenReturn(Optional.of(caddie));
        when(queueRepository.findByCaddie_IdAndQueueDateAndIsDeletedFalse(1L, date))
                .thenReturn(Optional.empty());

        // when
        var result = caddieMobileService.getMyQueue(caddy(20L, 10L), date);

        // then
        assertThat(result.queueNumber()).isNull();
        assertThat(result.queueDate()).isEqualTo(date);
    }

    // ─── 헬퍼 ──────────────────────────────────────────────────────────

    private AuthenticatedUser caddy(Long userId, Long golfCourseId) {
        return new AuthenticatedUser(userId, UserRole.CADDY, golfCourseId);
    }

    private AuthenticatedUser manager(Long golfCourseId) {
        return new AuthenticatedUser(10L, UserRole.MANAGER, golfCourseId);
    }

    private GolfCourse golfCourse(Long id) {
        GolfCourse gc = GolfCourse.create("테스트 골프장", "주소", "010-0000-0000");
        ReflectionTestUtils.setField(gc, "id", id);
        return gc;
    }

    private Caddie caddie(Long id, GolfCourse gc, String number) {
        Caddie c = Caddie.createOnApproval(gc, null, "캐디" + number);
        c.updateInfo(number, null, null);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
