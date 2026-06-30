package com.fairwaygms.fairwaygmsbe.operation.application.service;

import com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.CreateSpecialDayReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.req.UpdateRainPolicyReq;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.RainPolicyRes;
import com.fairwaygms.fairwaygmsbe.operation.application.model.res.SpecialDayRes;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.RainCancellationPolicy;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.SpecialOperationDay;
import com.fairwaygms.fairwaygmsbe.operation.domain.enums.RainCancellationPolicyType;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.RainCancellationPolicyRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.SpecialOperationDayRepository;
import com.fairwaygms.fairwaygmsbe.operation.exception.OperationErrorCode;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationPolicyServiceTest {

    @Mock private SpecialOperationDayRepository specialDayRepository;
    @Mock private RainCancellationPolicyRepository rainPolicyRepository;
    @Mock private GolfCourseRepository golfCourseRepository;
    @Mock private GolfCourseContextResolver contextResolver;

    private OperationPolicyService service;

    @BeforeEach
    void setUp() {
        service = new OperationPolicyService(specialDayRepository, rainPolicyRepository,
                golfCourseRepository, contextResolver);
    }

    // ── 특별 운영일 ─────────────────────────────────────────────────────────────

    @Test
    void createSpecialDay_succeeds() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        when(specialDayRepository.existsByGolfCourse_IdAndOperationDateAndIsDeletedFalse(
                10L, LocalDate.of(2025, 6, 1))).thenReturn(false);
        GolfCourse gc = golfCourse(10L);
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(gc));
        when(specialDayRepository.save(any())).thenAnswer(inv -> {
            SpecialOperationDay s = inv.getArgument(0);
            ReflectionTestUtils.setField(s, "id", 1L);
            return s;
        });

        CreateSpecialDayReq req = new CreateSpecialDayReq(LocalDate.of(2025, 6, 1), "현충일");

        // when
        SpecialDayRes res = service.createSpecialDay(req, auth);

        // then
        assertThat(res.operationDate()).isEqualTo(LocalDate.of(2025, 6, 1));
        assertThat(res.note()).isEqualTo("현충일");
    }

    @Test
    void createSpecialDay_failsOnDuplicate() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        when(specialDayRepository.existsByGolfCourse_IdAndOperationDateAndIsDeletedFalse(
                10L, LocalDate.of(2025, 6, 1))).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> service.createSpecialDay(
                new CreateSpecialDayReq(LocalDate.of(2025, 6, 1), "현충일"), auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(OperationErrorCode.SPECIAL_DAY_ALREADY_EXISTS));
    }

    @Test
    void createSpecialDay_failsWhenNotManager() {
        // when & then
        assertThatThrownBy(() -> service.createSpecialDay(
                new CreateSpecialDayReq(LocalDate.of(2025, 6, 1), "현충일"), admin()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    // ── 우천취소 정책 ──────────────────────────────────────────────────────────

    @Test
    void upsertRainPolicy_createsNewWhenNotExists() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        when(rainPolicyRepository.findByGolfCourse_IdAndIsDeletedFalse(10L)).thenReturn(Optional.empty());
        GolfCourse gc = golfCourse(10L);
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(gc));
        when(rainPolicyRepository.save(any())).thenAnswer(inv -> {
            RainCancellationPolicy p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 1L);
            return p;
        });

        // when
        RainPolicyRes res = service.upsertRainPolicy(
                new UpdateRainPolicyReq(RainCancellationPolicyType.KEEP_ORDER), auth);

        // then
        assertThat(res.policyType()).isEqualTo(RainCancellationPolicyType.KEEP_ORDER);
        verify(rainPolicyRepository).save(any(RainCancellationPolicy.class));
    }

    @Test
    void upsertRainPolicy_updatesExistingPolicy() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        GolfCourse gc = golfCourse(10L);
        RainCancellationPolicy existing = rainPolicy(1L, gc, RainCancellationPolicyType.KEEP_ORDER);
        when(rainPolicyRepository.findByGolfCourse_IdAndIsDeletedFalse(10L)).thenReturn(Optional.of(existing));

        // when
        RainPolicyRes res = service.upsertRainPolicy(
                new UpdateRainPolicyReq(RainCancellationPolicyType.RESEQUENCE), auth);

        // then — 기존 정책 타입이 변경되어야 함
        assertThat(existing.getPolicyType()).isEqualTo(RainCancellationPolicyType.RESEQUENCE);
        assertThat(res.policyType()).isEqualTo(RainCancellationPolicyType.RESEQUENCE);
    }

    @Test
    void getRainPolicy_failsWhenNotFound() {
        // given
        AuthenticatedUser auth = manager(10L);
        when(contextResolver.resolveTargetGolfCourseId(auth)).thenReturn(10L);
        when(rainPolicyRepository.findByGolfCourse_IdAndIsDeletedFalse(10L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.getRainPolicy(auth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(OperationErrorCode.RAIN_POLICY_NOT_FOUND));
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

    private RainCancellationPolicy rainPolicy(Long id, GolfCourse gc, RainCancellationPolicyType type) {
        RainCancellationPolicy p = RainCancellationPolicy.create(gc, type);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }
}
