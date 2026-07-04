package com.fairwaygms.fairwaygmsbe.settlement.integration;

import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentRepository;
import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Course;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.CourseRepository;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationPeriod;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.OperationSetting;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.ReservationTeam;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.TeeTime;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.OperationPeriodRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.OperationSettingRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.ReservationTeamRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.TeeTimeRepository;
import com.fairwaygms.fairwaygmsbe.settlement.application.model.req.AdjustCaddieFeeReq;
import com.fairwaygms.fairwaygmsbe.settlement.application.model.req.UpsertFeePolicyReq;
import com.fairwaygms.fairwaygmsbe.settlement.application.model.res.FeePolicyRes;
import com.fairwaygms.fairwaygmsbe.settlement.application.model.res.IncomeSummaryRes;
import com.fairwaygms.fairwaygmsbe.settlement.application.model.res.MonthlySettlementRes;
import com.fairwaygms.fairwaygmsbe.settlement.application.service.AssignmentRecordService;
import com.fairwaygms.fairwaygmsbe.settlement.application.service.FeePolicyService;
import com.fairwaygms.fairwaygmsbe.settlement.application.service.MonthlySettlementService;
import com.fairwaygms.fairwaygmsbe.settlement.domain.entity.AssignmentRecord;
import com.fairwaygms.fairwaygmsbe.settlement.domain.enums.CompletionType;
import com.fairwaygms.fairwaygmsbe.settlement.domain.enums.HalfBackType;
import com.fairwaygms.fairwaygmsbe.settlement.domain.enums.NoShowPolicy;
import com.fairwaygms.fairwaygmsbe.settlement.exception.SettlementErrorCode;
import com.fairwaygms.fairwaygmsbe.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SettlementFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired FeePolicyService feePolicyService;
    @Autowired AssignmentRecordService assignmentRecordService;
    @Autowired MonthlySettlementService monthlySettlementService;
    @Autowired GolfCourseRepository golfCourseRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired UserRepository userRepository;
    @Autowired CaddieRepository caddieRepository;
    @Autowired AssignmentRepository assignmentRepository;
    @Autowired OperationSettingRepository settingRepository;
    @Autowired OperationPeriodRepository periodRepository;
    @Autowired TeeTimeRepository teeTimeRepository;
    @Autowired ReservationTeamRepository teamRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private static final LocalDate TEST_DATE = LocalDate.of(2026, 7, 1);
    private static final String YEAR_MONTH = "2026-07";

    private GolfCourse golfCourse;
    private Caddie caddie;
    private AuthenticatedUser managerAuth;

    @BeforeEach
    void setUp() {
        golfCourse = golfCourseRepository.save(GolfCourse.create("테스트CC", "서울시", "02-0000-0000"));
        Course course = courseRepository.save(Course.create(golfCourse, "A코스", 18, 1));

        User managerUser = User.createEmailUser(
                "manager@test.com", passwordEncoder.encode("Manager1!"), "매니저",
                null, UserRole.MANAGER, golfCourse.getId());
        managerUser.approve(999L);
        managerUser = userRepository.save(managerUser);
        managerAuth = new AuthenticatedUser(managerUser.getId(), UserRole.MANAGER, golfCourse.getId());

        User caddieUser = User.createEmailUser(
                "caddie@test.com", passwordEncoder.encode("Caddie1!"), "캐디",
                null, UserRole.CADDY, golfCourse.getId());
        caddieUser.approve(999L);
        caddieUser = userRepository.save(caddieUser);
        caddie = caddieRepository.save(Caddie.createOnApproval(golfCourse, caddieUser, "홍길동"));

        OperationSetting setting = settingRepository.save(OperationSetting.create(golfCourse, "2026-07"));
        OperationPeriod period = periodRepository.save(OperationPeriod.create(
                setting, golfCourse, course, 1, LocalTime.of(7, 0), LocalTime.of(12, 0), 10));
        TeeTime teeTime = teeTimeRepository.save(
                TeeTime.create(golfCourse, period, course, TEST_DATE, LocalTime.of(8, 0)));
        teamRepository.save(ReservationTeam.create(golfCourse, teeTime, "홍팀", "홍길동", 4, null));
    }

    // ─── 캐디피 정책 ──────────────────────────────────────────────────────────

    @Test
    void 캐디피_정책_등록_후_조회() {
        // when
        feePolicyService.upsertFeePolicy(new UpsertFeePolicyReq(
                new BigDecimal("100000"), new BigDecimal("60000"),
                HalfBackType.SINGLE, NoShowPolicy.NONE, null, null), managerAuth);

        FeePolicyRes result = feePolicyService.getFeePolicy(managerAuth);

        // then
        assertThat(result.fullRoundFee()).isEqualByComparingTo("100000");
        assertThat(result.halfRoundFee()).isEqualByComparingTo("60000");
        assertThat(result.noShowPolicy()).isEqualTo("NONE");
    }

    @Test
    void 캐디피_정책_재등록_시_수정된다() {
        feePolicyService.upsertFeePolicy(new UpsertFeePolicyReq(
                new BigDecimal("100000"), null, null, NoShowPolicy.NONE, null, null), managerAuth);

        feePolicyService.upsertFeePolicy(new UpsertFeePolicyReq(
                new BigDecimal("120000"), new BigDecimal("70000"),
                HalfBackType.DOUBLE, NoShowPolicy.FULL, new BigDecimal("120000"), null), managerAuth);

        FeePolicyRes result = feePolicyService.getFeePolicy(managerAuth);
        assertThat(result.fullRoundFee()).isEqualByComparingTo("120000");
        assertThat(result.halfBackType()).isEqualTo("DOUBLE");
    }

    // ─── 배정 기록 생성 ───────────────────────────────────────────────────────

    @Test
    void 배정_기록_생성_후_중복_생성_시_예외() {
        Assignment assignment = saveAssignment();

        assignmentRecordService.createRecord(
                golfCourse.getId(), assignment.getId(), caddie.getId(),
                TEST_DATE, CompletionType.NORMAL, null, new BigDecimal("100000"));

        // 같은 assignmentId로 중복 생성 시 예외
        assertThatThrownBy(() ->
                assignmentRecordService.createRecord(
                        golfCourse.getId(), assignment.getId(), caddie.getId(),
                        TEST_DATE, CompletionType.NORMAL, null, new BigDecimal("100000")))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(SettlementErrorCode.ASSIGNMENT_RECORD_ALREADY_EXISTS));
    }

    // ─── 월별 정산 집계 ───────────────────────────────────────────────────────

    @Test
    void 월별_집계_확정_처리() {
        Assignment assignment = saveAssignment();
        assignmentRecordService.createRecord(
                golfCourse.getId(), assignment.getId(), caddie.getId(),
                TEST_DATE, CompletionType.NORMAL, null, new BigDecimal("100000"));

        // when
        MonthlySettlementRes result = monthlySettlementService.confirmMonth(YEAR_MONTH, managerAuth);

        // then
        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.confirmedAt()).isNotNull();
    }

    @Test
    void 마감_확정_후_재확정_시_예외() {
        Assignment assignment = saveAssignment();
        assignmentRecordService.createRecord(
                golfCourse.getId(), assignment.getId(), caddie.getId(),
                TEST_DATE, CompletionType.NORMAL, null, new BigDecimal("100000"));
        monthlySettlementService.confirmMonth(YEAR_MONTH, managerAuth);

        assertThatThrownBy(() -> monthlySettlementService.confirmMonth(YEAR_MONTH, managerAuth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(SettlementErrorCode.SETTLEMENT_ALREADY_CONFIRMED));
    }

    @Test
    void 마감_확정_후_수동_조정_시_예외() {
        Assignment assignment = saveAssignment();
        assignmentRecordService.createRecord(
                golfCourse.getId(), assignment.getId(), caddie.getId(),
                TEST_DATE, CompletionType.NORMAL, null, new BigDecimal("100000"));
        monthlySettlementService.confirmMonth(YEAR_MONTH, managerAuth);

        assertThatThrownBy(() ->
                monthlySettlementService.adjustCaddieFee(caddie.getId(),
                        new AdjustCaddieFeeReq(YEAR_MONTH, new BigDecimal("90000"), "조정 사유"),
                        managerAuth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(SettlementErrorCode.SETTLEMENT_ALREADY_CONFIRMED));
    }

    @Test
    void 수입_집계_조회() {
        Assignment assignment = saveAssignment();
        assignmentRecordService.createRecord(
                golfCourse.getId(), assignment.getId(), caddie.getId(),
                TEST_DATE, CompletionType.NORMAL, null, new BigDecimal("100000"));

        // when
        List<IncomeSummaryRes> result = monthlySettlementService.getIncomeSummary(YEAR_MONTH, managerAuth);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).caddieId()).isEqualTo(caddie.getId());
        assertThat(result.get(0).totalFee()).isEqualByComparingTo("100000");
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────

    private Assignment saveAssignment() {
        ReservationTeam team = teamRepository.findAll().get(0);
        Assignment assignment = Assignment.create(golfCourse, team, caddie, TEST_DATE, false, false);
        return assignmentRepository.save(assignment);
    }
}
