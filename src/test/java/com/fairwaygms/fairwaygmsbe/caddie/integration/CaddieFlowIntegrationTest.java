package com.fairwaygms.fairwaygmsbe.caddie.integration;

import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.AdjustQueueReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.InitializeQueueReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.RegisterDailyStatusReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.AvailableCaddieRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.InitializeQueueRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.QueueRes;
import com.fairwaygms.fairwaygmsbe.caddie.application.service.CaddieService;
import com.fairwaygms.fairwaygmsbe.caddie.application.service.DailyStatusService;
import com.fairwaygms.fairwaygmsbe.caddie.application.service.QueueService;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieWorkPattern;
import com.fairwaygms.fairwaygmsbe.caddie.domain.enums.DailyStatusType;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieWorkPatternRepository;
import com.fairwaygms.fairwaygmsbe.caddie.exception.CaddieErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaddieFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired CaddieService caddieService;
    @Autowired QueueService queueService;
    @Autowired DailyStatusService dailyStatusService;
    @Autowired CaddieRepository caddieRepository;
    @Autowired CaddieWorkPatternRepository workPatternRepository;
    @Autowired GolfCourseRepository golfCourseRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private GolfCourse golfCourse;
    private AuthenticatedUser managerAuth;
    private Long managerId;
    private static final LocalDate TEST_DATE = LocalDate.of(2026, 7, 1);

    @BeforeEach
    void setUp() {
        golfCourse = golfCourseRepository.save(GolfCourse.create("테스트 골프장", "서울시", "02-0000-0000"));

        // QueueService.findUser()에서 실제 User를 조회하므로 매니저 User를 DB에 저장한다
        User managerUser = User.createEmailUser("manager@test.com", passwordEncoder.encode("Manager1!"), "매니저", null, UserRole.MANAGER, golfCourse.getId());
        managerUser.approve(999L);
        managerUser = userRepository.save(managerUser);
        managerId = managerUser.getId();
        managerAuth = new AuthenticatedUser(managerId, UserRole.MANAGER, golfCourse.getId());
    }

    // ─── 순번 초기화 ──────────────────────────────────────────────────────────

    @Test
    void 순번_초기화_시_ACTIVE_캐디_수만큼_순번_생성() {
        // given — 캐디 3명 생성
        saveCaddie("A01");
        saveCaddie("A02");
        saveCaddie("A03");

        // when
        InitializeQueueRes result = queueService.initializeQueues(new InitializeQueueReq(TEST_DATE), managerAuth);

        // then
        assertThat(result.initializedCount()).isEqualTo(3);
        assertThat(result.queueDate()).isEqualTo(TEST_DATE);
    }

    @Test
    void 순번_초기화_시_캐디번호_오름차순으로_1번부터_부여() {
        // given — 캐디번호 역순으로 생성해도 초기화 후에는 오름차순
        saveCaddie("C01");
        saveCaddie("A01");
        saveCaddie("B01");

        // when
        queueService.initializeQueues(new InitializeQueueReq(TEST_DATE), managerAuth);
        List<QueueRes> queues = queueService.getQueues(golfCourse.getId(), TEST_DATE, managerAuth);

        // then — A01→1번, B01→2번, C01→3번
        assertThat(queues).hasSize(3);
        assertThat(queues.get(0).caddieNumber()).isEqualTo("A01");
        assertThat(queues.get(0).queueNumber()).isEqualTo(1);
        assertThat(queues.get(1).caddieNumber()).isEqualTo("B01");
        assertThat(queues.get(2).caddieNumber()).isEqualTo("C01");
    }

    @Test
    void 순번_초기화_재실행_시_기존_순번_삭제_후_재생성() {
        // given — 1차 초기화
        saveCaddie("A01");
        saveCaddie("A02");
        queueService.initializeQueues(new InitializeQueueReq(TEST_DATE), managerAuth);

        // when — 캐디 1명 추가 후 2차 초기화
        saveCaddie("A03");
        InitializeQueueRes result = queueService.initializeQueues(new InitializeQueueReq(TEST_DATE), managerAuth);

        // then — 기존 2건 삭제 + 신규 3건 생성 = 활성 3건
        assertThat(result.initializedCount()).isEqualTo(3);
        List<QueueRes> queues = queueService.getQueues(golfCourse.getId(), TEST_DATE, managerAuth);
        assertThat(queues).hasSize(3);
    }

    // ─── 순번 수동 조정 ──────────────────────────────────────────────────────

    @Test
    void 순번_수동_조정_성공() {
        // given
        Caddie c1 = saveCaddie("A01");
        saveCaddie("A02");
        saveCaddie("A03");
        queueService.initializeQueues(new InitializeQueueReq(TEST_DATE), managerAuth); // A01=1, A02=2, A03=3

        // when — A01을 3번으로 조정
        QueueRes result = queueService.adjustQueue(c1.getId(),
                new AdjustQueueReq(TEST_DATE, 5, "운영자 요청"), managerAuth);

        // then
        assertThat(result.queueNumber()).isEqualTo(5);
        assertThat(result.caddieNumber()).isEqualTo("A01");
    }

    @Test
    void 중복_순번으로_조정_거부() {
        // given
        Caddie c1 = saveCaddie("A01");
        saveCaddie("A02"); // 초기화 후 A02가 2번을 가짐
        queueService.initializeQueues(new InitializeQueueReq(TEST_DATE), managerAuth);

        // when & then — A01을 이미 A02가 가진 2번으로 조정 시도
        assertThatThrownBy(() -> queueService.adjustQueue(c1.getId(),
                new AdjustQueueReq(TEST_DATE, 2, "사유"), managerAuth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CaddieErrorCode.DUPLICATE_QUEUE_NUMBER));
    }

    @Test
    void 사유_없이_순번_조정_거부() {
        // given
        Caddie c1 = saveCaddie("A01");
        queueService.initializeQueues(new InitializeQueueReq(TEST_DATE), managerAuth);

        // when & then
        assertThatThrownBy(() -> queueService.adjustQueue(c1.getId(),
                new AdjustQueueReq(TEST_DATE, 5, null), managerAuth))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CaddieErrorCode.QUEUE_ADJUST_REASON_REQUIRED));
    }

    // ─── 일별 현황 → 가용 캐디 필터링 ────────────────────────────────────────

    @Test
    void 휴무_등록된_캐디는_가용_목록에서_제외() {
        // given
        Caddie c1 = saveCaddie("A01");
        saveCaddie("A02");

        dailyStatusService.register(new RegisterDailyStatusReq(
                c1.getId(), TEST_DATE, DailyStatusType.DAY_OFF, null, null), managerAuth);

        // when
        List<AvailableCaddieRes> available = caddieService.getAvailableCaddies(
                golfCourse.getId(), TEST_DATE, managerAuth);

        // then — A01 제외, A02만 가용
        assertThat(available).hasSize(1);
        assertThat(available.get(0).caddieNumber()).isEqualTo("A02");
    }

    @Test
    void 결근_등록된_캐디도_가용_목록에서_제외() {
        // given
        Caddie c1 = saveCaddie("A01");
        saveCaddie("A02");

        dailyStatusService.register(new RegisterDailyStatusReq(
                c1.getId(), TEST_DATE, DailyStatusType.ABSENCE, null, null), managerAuth);

        // when
        List<AvailableCaddieRes> available = caddieService.getAvailableCaddies(
                golfCourse.getId(), TEST_DATE, managerAuth);

        // then
        assertThat(available).hasSize(1);
        assertThat(available.get(0).caddieNumber()).isEqualTo("A02");
    }

    @Test
    void 가용_캐디에_순번_포함_조회() {
        // given
        saveCaddie("A01");
        saveCaddie("A02");
        queueService.initializeQueues(new InitializeQueueReq(TEST_DATE), managerAuth);

        // when
        List<AvailableCaddieRes> available = caddieService.getAvailableCaddies(
                golfCourse.getId(), TEST_DATE, managerAuth);

        // then — 순번이 포함되어 반환
        assertThat(available).hasSize(2);
        assertThat(available).allMatch(r -> r.queueNumber() != null);
    }

    // ─── 캐디 퇴사 처리 연동 ─────────────────────────────────────────────────

    @Test
    void 캐디_퇴사_시_user_계정도_WITHDRAWN_처리() {
        // given — CADDY user와 caddie 레코드 생성
        User caddyUser = User.createEmailUser("caddy@test.com", passwordEncoder.encode("Password1!"), "캐디홍", null, UserRole.CADDY, golfCourse.getId());
        caddyUser.approve(999L);
        caddyUser = userRepository.save(caddyUser);

        Caddie caddie = caddieService.createOnApproval(caddyUser);
        caddie.updateInfo("A01", null, null);

        // when
        caddieService.withdrawCaddie(caddie.getId(), managerAuth);

        // then — User.withdraw()는 isDeleted=true로 설정하므로 findById로 조회한다
        User withdrawn = userRepository.findById(caddyUser.getId()).orElseThrow();
        assertThat(withdrawn.getStatus().name()).isEqualTo("WITHDRAWN");
        assertThat(withdrawn.getIsDeleted()).isTrue();
        assertThat(caddie.getStatus().name()).isEqualTo("RESIGNED");
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────

    private Caddie saveCaddie(String caddieNumber) {
        Caddie caddie = Caddie.createOnApproval(golfCourse, null, "캐디" + caddieNumber);
        caddie.updateInfo(caddieNumber, null, null);
        caddieRepository.save(caddie);
        CaddieWorkPattern pattern = CaddieWorkPattern.createDefault(caddie, golfCourse);
        workPatternRepository.save(pattern);
        return caddie;
    }
}
