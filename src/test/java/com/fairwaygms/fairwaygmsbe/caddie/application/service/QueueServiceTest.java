package com.fairwaygms.fairwaygmsbe.caddie.application.service;

import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.AdjustQueueReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.InitializeQueueReq;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieQueue;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.CaddieStatus;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieQueueHistoryRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieQueueRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.caddie.exception.CaddieErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    @Mock private CaddieRepository caddieRepository;
    @Mock private CaddieQueueRepository queueRepository;
    @Mock private CaddieQueueHistoryRepository queueHistoryRepository;
    @Mock private GolfCourseRepository golfCourseRepository;
    @Mock private UserRepository userRepository;

    private QueueService queueService;

    private static final LocalDate DATE = LocalDate.of(2026, 7, 1);

    @BeforeEach
    void setUp() {
        queueService = new QueueService(
                caddieRepository, queueRepository, queueHistoryRepository,
                golfCourseRepository, userRepository
        );
    }

    // ─── 순번 초기화 ──────────────────────────────────────────────────

    @Test
    void 순번_초기화_기존_순번_소프트삭제_후_재생성() {
        // given
        GolfCourse gc = golfCourse(10L);
        Caddie c1 = caddie(1L, gc, "A01");
        Caddie c2 = caddie(2L, gc, "A02");
        CaddieQueue existingQ1 = CaddieQueue.create(c1, gc, DATE, 1);
        CaddieQueue existingQ2 = CaddieQueue.create(c2, gc, DATE, 2);
        User manager = managerUser(10L);
        AuthenticatedUser auth = manager(10L);

        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(gc));
        when(userRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(manager));
        when(queueRepository.findForUpdateByGolfCourseAndDate(10L, DATE))
                .thenReturn(List.of(existingQ1, existingQ2));
        when(caddieRepository.findByGolfCourse_IdAndStatusAndIsDeletedFalse(10L, CaddieStatus.ACTIVE))
                .thenReturn(List.of(c1, c2));
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(queueHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        var result = queueService.initializeQueues(new InitializeQueueReq(DATE), auth);

        // then — 기존 순번 소프트 삭제
        assertThat(existingQ1.getIsDeleted()).isTrue();
        assertThat(existingQ2.getIsDeleted()).isTrue();
        // 초기화된 캐디 수
        assertThat(result.initializedCount()).isEqualTo(2);
        assertThat(result.queueDate()).isEqualTo(DATE);
        // 신규 CaddieQueue 2건 + 이력 2건 저장
        verify(queueRepository, times(2)).save(any(CaddieQueue.class));
        verify(queueHistoryRepository, times(2)).save(any());
    }

    @Test
    void 순번_초기화_캐디번호_오름차순_정렬() {
        // given — A02가 먼저 있어도 A01이 1번이 되어야 함
        GolfCourse gc = golfCourse(10L);
        Caddie c1 = caddie(1L, gc, "A01");
        Caddie c2 = caddie(2L, gc, "A02");
        User manager = managerUser(10L);

        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(gc));
        when(userRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(manager));
        when(queueRepository.findForUpdateByGolfCourseAndDate(10L, DATE)).thenReturn(List.of());
        // A02, A01 역순으로 반환
        when(caddieRepository.findByGolfCourse_IdAndStatusAndIsDeletedFalse(10L, CaddieStatus.ACTIVE))
                .thenReturn(List.of(c2, c1));

        List<CaddieQueue> saved = new java.util.ArrayList<>();
        when(queueRepository.save(any())).thenAnswer(inv -> {
            saved.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(queueHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        queueService.initializeQueues(new InitializeQueueReq(DATE), manager(10L));

        // then — A01(id=1)이 1번, A02(id=2)가 2번
        assertThat(saved.get(0).getCaddie().getCaddieNumber()).isEqualTo("A01");
        assertThat(saved.get(0).getQueueNumber()).isEqualTo(1);
        assertThat(saved.get(1).getCaddie().getCaddieNumber()).isEqualTo("A02");
        assertThat(saved.get(1).getQueueNumber()).isEqualTo(2);
    }

    // ─── 수동 조정 ────────────────────────────────────────────────────

    @Test
    void 수동_조정_사유_없으면_예외() {
        // given — reason이 null
        var req = new AdjustQueueReq(DATE, 3, null);

        // when & then
        assertThatThrownBy(() -> queueService.adjustQueue(1L, req, manager(10L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CaddieErrorCode.QUEUE_ADJUST_REASON_REQUIRED));
    }

    @Test
    void 수동_조정_빈_사유도_예외() {
        var req = new AdjustQueueReq(DATE, 3, "  ");

        assertThatThrownBy(() -> queueService.adjustQueue(1L, req, manager(10L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CaddieErrorCode.QUEUE_ADJUST_REASON_REQUIRED));
    }

    @Test
    void 수동_조정_중복_순번이면_예외() {
        // given
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = caddie(1L, gc, "A01");
        CaddieQueue queue = CaddieQueue.create(caddie, gc, DATE, 2);
        User manager = managerUser(10L);

        when(caddieRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(caddie));
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(gc));
        when(userRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(manager));
        when(queueRepository.findForUpdateByGolfCourseAndDate(10L, DATE)).thenReturn(List.of(queue));
        when(queueRepository.findByCaddie_IdAndQueueDateAndIsDeletedFalse(1L, DATE)).thenReturn(Optional.of(queue));
        // 목표 순번 5가 이미 사용 중
        when(queueRepository.existsByGolfCourse_IdAndQueueDateAndQueueNumberAndIsDeletedFalse(10L, DATE, 5))
                .thenReturn(true);

        var req = new AdjustQueueReq(DATE, 5, "조정 사유");

        // when & then
        assertThatThrownBy(() -> queueService.adjustQueue(1L, req, manager(10L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CaddieErrorCode.DUPLICATE_QUEUE_NUMBER));
    }

    @Test
    void 수동_조정_정상_처리() {
        // given
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = caddie(1L, gc, "A01");
        CaddieQueue queue = CaddieQueue.create(caddie, gc, DATE, 2);
        User manager = managerUser(10L);

        when(caddieRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(caddie));
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(gc));
        when(userRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(manager));
        when(queueRepository.findForUpdateByGolfCourseAndDate(10L, DATE)).thenReturn(List.of(queue));
        when(queueRepository.findByCaddie_IdAndQueueDateAndIsDeletedFalse(1L, DATE)).thenReturn(Optional.of(queue));
        when(queueRepository.existsByGolfCourse_IdAndQueueDateAndQueueNumberAndIsDeletedFalse(10L, DATE, 5))
                .thenReturn(false);
        when(queueHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        var result = queueService.adjustQueue(1L, new AdjustQueueReq(DATE, 5, "고객 요청으로 순번 조정"), manager(10L));

        // then
        assertThat(result.queueNumber()).isEqualTo(5);
        assertThat(queue.getQueueNumber()).isEqualTo(5);
        verify(queueHistoryRepository).save(any());
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

    private Caddie caddie(Long id, GolfCourse gc, String number) {
        Caddie c = Caddie.createOnApproval(gc, null, "캐디" + number);
        c.updateInfo(number, null, null);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private User managerUser(Long id) {
        User u = User.createEmailUser("manager@test.com", "pw", "매니저", "010-0000-0000",
                UserRole.MANAGER, 10L);
        ReflectionTestUtils.setField(u, "id", id);
        u.approve(1L);
        return u;
    }
}
