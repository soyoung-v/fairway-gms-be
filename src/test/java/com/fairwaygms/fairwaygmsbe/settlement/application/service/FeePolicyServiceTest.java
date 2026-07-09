package com.fairwaygms.fairwaygmsbe.settlement.application.service;

import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentRepository;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.settlement.application.model.req.UpsertFeePolicyReq;
import com.fairwaygms.fairwaygmsbe.settlement.application.model.res.FeePolicyRes;
import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.CaddieFeepolicy;
import com.fairwaygms.fairwaygmsbe.settlement.domain.enums.HalfBackType;
import com.fairwaygms.fairwaygmsbe.settlement.domain.enums.NoShowPolicy;
import com.fairwaygms.fairwaygmsbe.settlement.domain.repository.CaddieFeepolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeePolicyServiceTest {

    @Mock private CaddieFeepolicyRepository feePolicyRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver contextResolver;

    private FeePolicyService feePolicyService;

    private static final Long GOLF_COURSE_ID = 100L;
    private AuthenticatedUser managerAuth;

    // 18홀 10만원, 9홀 6만원 정책
    private CaddieFeepolicy policy;

    @BeforeEach
    void setUp() {
        feePolicyService = new FeePolicyService(feePolicyRepository, assignmentRepository, contextResolver);
        managerAuth = new AuthenticatedUser(1L, UserRole.MANAGER, GOLF_COURSE_ID);

        policy = CaddieFeepolicy.create(GOLF_COURSE_ID,
                new BigDecimal("100000"),
                new BigDecimal("60000"),
                HalfBackType.SINGLE,
                NoShowPolicy.HALF,
                new BigDecimal("60000"),
                null);
    }

    // ─── 우천취소 캐디피 계산 ───────────────────────────────────────────────────

    @Test
    void computeRainFee_전체라운드_fullRoundFee_반환() {
        BigDecimal fee = feePolicyService.computeRainFee(policy, 18, false);
        assertThat(fee).isEqualByComparingTo("100000");
    }

    @Test
    void computeRainFee_9홀_halfRoundFee_반환() {
        BigDecimal fee = feePolicyService.computeRainFee(policy, 9, false);
        assertThat(fee).isEqualByComparingTo("50000");
    }

    @Test
    void computeRainFee_진행홀수_비율_계산() {
        // 6홀 / 18홀 = 1/3 → 33333.33
        BigDecimal fee = feePolicyService.computeRainFee(policy, 6, false);
        assertThat(fee).isEqualByComparingTo("33333.33");
    }

    @Test
    void computeRainFee_0홀_0원_반환() {
        BigDecimal fee = feePolicyService.computeRainFee(policy, 0, false);
        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void computeRainFee_하프백_9홀기준으로_계산() {
        // isHalfBack=true: baseHoles=9, 9홀이면 halfRoundFee
        BigDecimal fee = feePolicyService.computeRainFee(policy, 9, true);
        assertThat(fee).isEqualByComparingTo("60000");
    }

    // ─── 노쇼 캐디피 계산 ─────────────────────────────────────────────────────

    @Test
    void computeNoShowFee_NONE_정책_0원() {
        CaddieFeepolicy nonePolicy = CaddieFeepolicy.create(GOLF_COURSE_ID,
                new BigDecimal("100000"), null, null, NoShowPolicy.NONE, null, null);
        BigDecimal fee = feePolicyService.computeNoShowFee(nonePolicy);
        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void computeNoShowFee_HALF_noShowFee_설정_시_해당값_반환() {
        BigDecimal fee = feePolicyService.computeNoShowFee(policy);
        assertThat(fee).isEqualByComparingTo("60000");
    }

    @Test
    void computeNoShowFee_FULL_정책_fullRoundFee_반환() {
        CaddieFeepolicy fullPolicy = CaddieFeepolicy.create(GOLF_COURSE_ID,
                new BigDecimal("100000"), null, null, NoShowPolicy.FULL, null, null);
        BigDecimal fee = feePolicyService.computeNoShowFee(fullPolicy);
        assertThat(fee).isEqualByComparingTo("100000");
    }

    // ─── 정책 upsert ─────────────────────────────────────────────────────────

    @Test
    void upsertFeePolicy_정책이_없으면_새로_저장한다() {
        when(feePolicyRepository.findByGolfCourseIdAndIsDeletedFalse(GOLF_COURSE_ID))
                .thenReturn(Optional.empty());
        when(feePolicyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpsertFeePolicyReq req = new UpsertFeePolicyReq(
                new BigDecimal("100000"), new BigDecimal("60000"),
                HalfBackType.SINGLE, NoShowPolicy.NONE, null, null);

        FeePolicyRes result = feePolicyService.upsertFeePolicy(req, managerAuth);

        verify(feePolicyRepository).save(any(CaddieFeepolicy.class));
        assertThat(result.fullRoundFee()).isEqualByComparingTo("100000");
    }

    @Test
    void upsertFeePolicy_기존_정책이_있으면_update_후_저장하지_않는다() {
        when(feePolicyRepository.findByGolfCourseIdAndIsDeletedFalse(GOLF_COURSE_ID))
                .thenReturn(Optional.of(policy));

        UpsertFeePolicyReq req = new UpsertFeePolicyReq(
                new BigDecimal("120000"), new BigDecimal("70000"),
                HalfBackType.DOUBLE, NoShowPolicy.FULL, new BigDecimal("120000"), null);

        FeePolicyRes result = feePolicyService.upsertFeePolicy(req, managerAuth);

        verify(feePolicyRepository, never()).save(any());
        assertThat(result.fullRoundFee()).isEqualByComparingTo("120000");
        assertThat(result.halfBackType()).isEqualTo("DOUBLE");
    }
}
