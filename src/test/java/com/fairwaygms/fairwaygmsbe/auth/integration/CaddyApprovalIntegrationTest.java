package com.fairwaygms.fairwaygmsbe.auth.integration;

import com.fairwaygms.fairwaygmsbe.auth.application.model.req.SignupReq;
import com.fairwaygms.fairwaygmsbe.auth.application.service.AdminUserService;
import com.fairwaygms.fairwaygmsbe.auth.application.service.AuthService;
import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.enums.UserStatus;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.auth.exception.AuthErrorCode;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieWorkPatternRepository;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// auth → caddie 도메인 연동 흐름 검증
class CaddyApprovalIntegrationTest extends AbstractIntegrationTest {

    @Autowired AdminUserService adminUserService;
    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;
    @Autowired CaddieRepository caddieRepository;
    @Autowired CaddieWorkPatternRepository workPatternRepository;
    @Autowired GolfCourseRepository golfCourseRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Long adminUserId;
    private AuthenticatedUser adminAuth;
    private Long golfCourseId;

    @BeforeEach
    void setUp() {
        GolfCourse gc = golfCourseRepository.save(GolfCourse.create("테스트 골프장", "서울시", "02-0000-0000"));
        golfCourseId = gc.getId();

        User admin = User.createInitialAdmin("admin@test.com", passwordEncoder.encode("Admin1!@"), "관리자");
        admin = userRepository.save(admin);
        adminUserId = admin.getId();
        adminAuth = new AuthenticatedUser(adminUserId, UserRole.ADMIN, null);
    }

    // ─── CADDY 승인 → caddie 레코드 생성 연동 ───────────────────────────────

    @Test
    void CADDY_승인_시_Caddie_레코드_생성() {
        // given
        authService.signup(new SignupReq("caddy1@example.com", "Password1!", "캐디홍", null, UserRole.CADDY, golfCourseId));
        User caddy = userRepository.findByEmailAndIsDeletedFalse("caddy1@example.com").orElseThrow();

        // when
        adminUserService.approveUser(adminAuth, caddy.getId());

        // then
        List<Caddie> caddies = caddieRepository.findByGolfCourse_IdAndIsDeletedFalse(golfCourseId);
        assertThat(caddies).hasSize(1);
        assertThat(caddies.get(0).getName()).isEqualTo("캐디홍");
        assertThat(caddies.get(0).getUser().getId()).isEqualTo(caddy.getId());
    }

    @Test
    void CADDY_승인_시_기본_WorkPattern_자동_생성() {
        // given
        authService.signup(new SignupReq("caddy2@example.com", "Password1!", "캐디이", null, UserRole.CADDY, golfCourseId));
        User caddy = userRepository.findByEmailAndIsDeletedFalse("caddy2@example.com").orElseThrow();

        // when
        adminUserService.approveUser(adminAuth, caddy.getId());

        // then
        Caddie savedCaddie = caddieRepository.findByGolfCourse_IdAndIsDeletedFalse(golfCourseId).get(0);
        var pattern = workPatternRepository.findByCaddie_IdAndIsDeletedFalse(savedCaddie.getId());
        assertThat(pattern).isPresent();
        assertThat(pattern.get().getCanWeekday()).isTrue();
        assertThat(pattern.get().getCanWeekend()).isTrue();
    }

    @Test
    void CADDY_승인_후_User_상태_ACTIVE_변경() {
        // given
        authService.signup(new SignupReq("caddy3@example.com", "Password1!", "캐디박", null, UserRole.CADDY, golfCourseId));
        User caddy = userRepository.findByEmailAndIsDeletedFalse("caddy3@example.com").orElseThrow();
        assertThat(caddy.getStatus()).isEqualTo(UserStatus.PENDING);

        // when
        adminUserService.approveUser(adminAuth, caddy.getId());

        // then
        User approved = userRepository.findByIdAndIsDeletedFalse(caddy.getId()).orElseThrow();
        assertThat(approved.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void MANAGER_승인_시_Caddie_레코드_미생성() {
        // given — MANAGER는 caddie 레코드 생성 대상이 아님
        authService.signup(new SignupReq("manager1@example.com", "Password1!", "매니저김", null, UserRole.MANAGER, golfCourseId));
        User manager = userRepository.findByEmailAndIsDeletedFalse("manager1@example.com").orElseThrow();

        // when
        adminUserService.approveUser(adminAuth, manager.getId());

        // then
        List<Caddie> caddies = caddieRepository.findByGolfCourse_IdAndIsDeletedFalse(golfCourseId);
        assertThat(caddies).isEmpty();
    }

    @Test
    void 이미_승인된_계정_재승인_거부() {
        // given
        authService.signup(new SignupReq("caddy4@example.com", "Password1!", "캐디최", null, UserRole.CADDY, golfCourseId));
        User caddy = userRepository.findByEmailAndIsDeletedFalse("caddy4@example.com").orElseThrow();
        adminUserService.approveUser(adminAuth, caddy.getId());

        // when & then
        assertThatThrownBy(() -> adminUserService.approveUser(adminAuth, caddy.getId()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.ALREADY_PROCESSED));
    }

    @Test
    void CADDY_거절_시_Caddie_레코드_미생성() {
        // given
        authService.signup(new SignupReq("caddy5@example.com", "Password1!", "캐디정", null, UserRole.CADDY, golfCourseId));
        User caddy = userRepository.findByEmailAndIsDeletedFalse("caddy5@example.com").orElseThrow();

        // when
        adminUserService.rejectUser(adminAuth, caddy.getId());

        // then
        List<Caddie> caddies = caddieRepository.findByGolfCourse_IdAndIsDeletedFalse(golfCourseId);
        assertThat(caddies).isEmpty();
        User rejected = userRepository.findByIdAndIsDeletedFalse(caddy.getId()).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(UserStatus.REJECTED);
    }
}
