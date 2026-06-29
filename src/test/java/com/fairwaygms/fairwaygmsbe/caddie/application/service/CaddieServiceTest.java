package com.fairwaygms.fairwaygmsbe.caddie.application.service;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.enums.UserStatus;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.ChangeCaddieStatusReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.UpdateCaddieReq;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieQueue;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class CaddieServiceTest {

    @Mock private CaddieRepository caddieRepository;
    @Mock private CaddieWorkPatternRepository workPatternRepository;
    @Mock private CaddieDailyStatusRepository dailyStatusRepository;
    @Mock private CaddieQueueRepository queueRepository;
    @Mock private GolfCourseRepository golfCourseRepository;
    @Mock private UserRepository userRepository;

    private CaddieService caddieService;

    @BeforeEach
    void setUp() {
        caddieService = new CaddieService(
                caddieRepository, workPatternRepository, dailyStatusRepository,
                queueRepository, golfCourseRepository, userRepository
        );
    }

    // ─── 정보 수정 ───────────────────────────────────────────────────

    @Test
    void updateInfo_번호_중복이면_예외() {
        // given
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = activeCaddie(1L, gc, "A01");
        AuthenticatedUser manager = manager(10L);

        when(caddieRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(caddie));
        when(caddieRepository.existsByGolfCourse_IdAndCaddieNumberAndIsDeletedFalse(10L, "B99"))
                .thenReturn(true);

        // when & then
        assertThatThrownBy(() -> caddieService.updateInfo(1L, new UpdateCaddieReq("B99", null, null), manager))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CaddieErrorCode.DUPLICATE_CADDIE_NUMBER));
    }

    @Test
    void updateInfo_같은_번호_유지시_중복_검사_스킵() {
        // given
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = activeCaddie(1L, gc, "A01");
        AuthenticatedUser manager = manager(10L);

        when(caddieRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(caddie));

        // when — 번호 그대로 유지(변경 없음), 중복 검사 쿼리는 호출되면 안 됨
        caddieService.updateInfo(1L, new UpdateCaddieReq("A01", "010-9999-8888", null), manager);

        verify(caddieRepository, never())
                .existsByGolfCourse_IdAndCaddieNumberAndIsDeletedFalse(any(), any());
    }

    // ─── 상태 변경 ───────────────────────────────────────────────────

    @Test
    void changeStatus_RESIGNED_직접_요청시_예외() {
        // given
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = activeCaddie(1L, gc, "A01");
        AuthenticatedUser manager = manager(10L);

        when(caddieRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(caddie));

        // when & then — 퇴사는 /withdraw API를 통해서만 처리해야 함
        assertThatThrownBy(() -> caddieService.changeStatus(1L, new ChangeCaddieStatusReq(CaddieStatus.RESIGNED), manager))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CaddieErrorCode.INVALID_CADDIE_STATUS));
    }

    @Test
    void changeStatus_ON_LEAVE_정상_변경() {
        // given
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = activeCaddie(1L, gc, "A01");
        AuthenticatedUser manager = manager(10L);

        when(caddieRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(caddie));

        // when
        caddieService.changeStatus(1L, new ChangeCaddieStatusReq(CaddieStatus.ON_LEAVE), manager);

        // then
        assertThat(caddie.getStatus()).isEqualTo(CaddieStatus.ON_LEAVE);
    }

    // ─── 권한 검증 ───────────────────────────────────────────────────

    @Test
    void MANAGER_전용_API를_ADMIN이_호출하면_FORBIDDEN() {
        // given — ADMIN은 Manager 전용 수정 API에 접근 불가
        // validateManager()가 repo 조회 전에 예외를 던지므로 stub 불필요
        AuthenticatedUser admin = admin();

        // when & then
        assertThatThrownBy(() -> caddieService.changeStatus(1L, new ChangeCaddieStatusReq(CaddieStatus.ON_LEAVE), admin))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void 다른_골프장_캐디_접근시_FORBIDDEN() {
        // given — Manager(골프장 10)가 골프장 99 소속 캐디에 접근 불가
        GolfCourse gc = golfCourse(99L);
        Caddie caddie = activeCaddie(1L, gc, "A01");
        AuthenticatedUser manager = manager(10L);

        when(caddieRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(caddie));

        // when & then
        assertThatThrownBy(() -> caddieService.changeStatus(1L, new ChangeCaddieStatusReq(CaddieStatus.ON_LEAVE), manager))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    // ─── 퇴사 처리 ───────────────────────────────────────────────────

    @Test
    void withdrawCaddie_user_계정도_WITHDRAWN_처리() {
        // given
        GolfCourse gc = golfCourse(10L);
        User linkedUser = caddyUser(5L, 10L);
        Caddie caddie = activeCaddieWithUser(1L, gc, "A01", linkedUser);
        AuthenticatedUser manager = manager(10L);

        when(caddieRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(caddie));
        when(userRepository.findByIdAndIsDeletedFalse(5L)).thenReturn(Optional.of(linkedUser));

        // when
        caddieService.withdrawCaddie(1L, manager);

        // then
        assertThat(caddie.getStatus()).isEqualTo(CaddieStatus.RESIGNED);
        assertThat(caddie.getIsDeleted()).isTrue();
        assertThat(linkedUser.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
    }

    @Test
    void withdrawCaddie_user_미연동_캐디도_정상_퇴사() {
        // given — user가 없는 캐디(가입 전 등록)
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = activeCaddie(1L, gc, "A01");
        AuthenticatedUser manager = manager(10L);

        when(caddieRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(caddie));

        // when
        caddieService.withdrawCaddie(1L, manager);

        // then
        assertThat(caddie.getStatus()).isEqualTo(CaddieStatus.RESIGNED);
        verify(userRepository, never()).findByIdAndIsDeletedFalse(any());
    }

    // ─── 가용 캐디 조회 ───────────────────────────────────────────────

    @Test
    void 배정_제외_유형_있으면_가용_캐디에서_제외() {
        // given
        GolfCourse gc = golfCourse(10L);
        Caddie available = activeCaddie(1L, gc, "A01");
        Caddie excluded = activeCaddie(2L, gc, "A02");
        AuthenticatedUser manager = manager(10L);
        LocalDate date = LocalDate.of(2026, 7, 1);

        when(caddieRepository.findByGolfCourse_IdAndStatusAndIsDeletedFalse(10L, CaddieStatus.ACTIVE))
                .thenReturn(List.of(available, excluded));
        when(queueRepository.findByGolfCourse_IdAndQueueDateAndIsDeletedFalseOrderByQueueNumberAsc(10L, date))
                .thenReturn(List.of());

        // available(1L) — 제외 유형 없음
        when(dailyStatusRepository.existsByCaddie_IdAndStatusDateAndTypeAndIsDeletedFalse(1L, date, DailyStatusType.DAY_OFF)).thenReturn(false);
        when(dailyStatusRepository.existsByCaddie_IdAndStatusDateAndTypeAndIsDeletedFalse(1L, date, DailyStatusType.ABSENCE)).thenReturn(false);
        when(dailyStatusRepository.existsByCaddie_IdAndStatusDateAndTypeAndIsDeletedFalse(1L, date, DailyStatusType.ASSIGN_EXCLUDED)).thenReturn(false);

        // excluded(2L) — noneMatch가 단락 평가로 DAY_OFF=true 발견 즉시 중단하므로 ABSENCE/ASSIGN_EXCLUDED는 호출 안 될 수 있음.
        // lenient()로 UnnecessaryStubbingException 방지
        Mockito.lenient().when(dailyStatusRepository.existsByCaddie_IdAndStatusDateAndTypeAndIsDeletedFalse(2L, date, DailyStatusType.DAY_OFF)).thenReturn(true);
        Mockito.lenient().when(dailyStatusRepository.existsByCaddie_IdAndStatusDateAndTypeAndIsDeletedFalse(2L, date, DailyStatusType.ABSENCE)).thenReturn(false);
        Mockito.lenient().when(dailyStatusRepository.existsByCaddie_IdAndStatusDateAndTypeAndIsDeletedFalse(2L, date, DailyStatusType.ASSIGN_EXCLUDED)).thenReturn(false);

        // when
        var result = caddieService.getAvailableCaddies(10L, date, manager);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    @Test
    void 가용_캐디_순번_정보_포함() {
        // given
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = activeCaddie(1L, gc, "A01");
        AuthenticatedUser manager = manager(10L);
        LocalDate date = LocalDate.of(2026, 7, 1);

        CaddieQueue queue = mockQueue(caddie, gc, date, 3);

        when(caddieRepository.findByGolfCourse_IdAndStatusAndIsDeletedFalse(10L, CaddieStatus.ACTIVE))
                .thenReturn(List.of(caddie));
        when(queueRepository.findByGolfCourse_IdAndQueueDateAndIsDeletedFalseOrderByQueueNumberAsc(10L, date))
                .thenReturn(List.of(queue));
        when(dailyStatusRepository.existsByCaddie_IdAndStatusDateAndTypeAndIsDeletedFalse(eq(1L), eq(date), any()))
                .thenReturn(false);

        // when
        var result = caddieService.getAvailableCaddies(10L, date, manager);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).queueNumber()).isEqualTo(3);
    }

    // ─── 헬퍼 ───────────────────────────────────────────────────────

    private AuthenticatedUser admin() {
        return new AuthenticatedUser(99L, UserRole.ADMIN, null);
    }

    private AuthenticatedUser manager(Long golfCourseId) {
        return new AuthenticatedUser(10L, UserRole.MANAGER, golfCourseId);
    }

    private GolfCourse golfCourse(Long id) {
        GolfCourse gc = GolfCourse.create("테스트 골프장", "주소", "010-0000-0000");
        ReflectionTestUtils.setField(gc, "id", id);
        return gc;
    }

    private Caddie activeCaddie(Long id, GolfCourse gc, String number) {
        User dummyUser = caddyUser(id + 100L, gc.getId());
        Caddie caddie = Caddie.createOnApproval(gc, null, "테스트 캐디 " + number);
        caddie.updateInfo(number, null, null);
        ReflectionTestUtils.setField(caddie, "id", id);
        return caddie;
    }

    private Caddie activeCaddieWithUser(Long id, GolfCourse gc, String number, User user) {
        Caddie caddie = Caddie.createOnApproval(gc, user, user.getName());
        caddie.updateInfo(number, null, null);
        ReflectionTestUtils.setField(caddie, "id", id);
        return caddie;
    }

    private User caddyUser(Long id, Long golfCourseId) {
        User user = User.createEmailUser(
                "caddy" + id + "@test.com", "pw", "캐디" + id, "010-0000-0000",
                UserRole.CADDY, golfCourseId
        );
        ReflectionTestUtils.setField(user, "id", id);
        user.approve(1L);
        return user;
    }

    private CaddieQueue mockQueue(Caddie caddie, GolfCourse gc, LocalDate date, int number) {
        CaddieQueue queue = CaddieQueue.create(caddie, gc, date, number);
        return queue;
    }
}
