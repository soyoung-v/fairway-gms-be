package com.fairwaygms.fairwaygmsbe.auth.integration;

import com.fairwaygms.fairwaygmsbe.auth.application.model.req.LoginReq;
import com.fairwaygms.fairwaygmsbe.auth.application.model.req.SignupReq;
import com.fairwaygms.fairwaygmsbe.auth.application.service.AuthService;
import com.fairwaygms.fairwaygmsbe.auth.application.service.AuthLoginResult;
import com.fairwaygms.fairwaygmsbe.auth.domain.entity.RefreshToken;
import com.fairwaygms.fairwaygmsbe.auth.domain.entity.User;
import com.fairwaygms.fairwaygmsbe.auth.domain.enums.UserStatus;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.RefreshTokenRepository;
import com.fairwaygms.fairwaygmsbe.auth.domain.repository.UserRepository;
import com.fairwaygms.fairwaygmsbe.auth.exception.AuthErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    // ─── 회원가입 ───────────────────────────────────────────────────────────

    @Test
    void 회원가입_성공_후_PENDING_상태로_DB_저장() {
        // given
        SignupReq req = new SignupReq("signup@example.com", "Password1!", "홍길동", "010-1111-0001", UserRole.MANAGER, 1L);

        // when
        authService.signup(req);

        // then
        User saved = userRepository.findByEmailAndIsDeletedFalse("signup@example.com").orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(saved.getName()).isEqualTo("홍길동");
        assertThat(saved.getRole()).isEqualTo(UserRole.MANAGER);
    }

    @Test
    void 이메일_중복_회원가입_거부() {
        // given
        SignupReq req = new SignupReq("dup@example.com", "Password1!", "홍길동", null, UserRole.MANAGER, 1L);
        authService.signup(req);

        // when & then
        assertThatThrownBy(() -> authService.signup(req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.EMAIL_DUPLICATED));
    }

    @Test
    void ADMIN_회원가입은_golfCourseId_없어도_성공() {
        // given
        SignupReq req = new SignupReq("admin@example.com", "Password1!", "관리자", null, UserRole.ADMIN, null);

        // when
        authService.signup(req);

        // then
        User saved = userRepository.findByEmailAndIsDeletedFalse("admin@example.com").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(saved.getGolfCourseId()).isNull();
    }

    // ─── 로그인 ──────────────────────────────────────────────────────────────

    @Test
    void PENDING_계정_로그인_거부() {
        // given
        authService.signup(new SignupReq("pending@example.com", "Password1!", "홍길동", null, UserRole.MANAGER, 1L));

        // when & then
        assertThatThrownBy(() -> authService.login(new LoginReq("pending@example.com", "Password1!")))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.ACCOUNT_PENDING));
    }

    @Test
    void ACTIVE_계정_로그인_성공_후_RefreshToken_DB_저장() {
        // given — ACTIVE 계정 직접 생성
        User user = activeUser("login@example.com", "Password1!");

        // when
        AuthLoginResult result = authService.login(new LoginReq("login@example.com", "Password1!"));

        // then
        assertThat(result.user().email()).isEqualTo("login@example.com");
        List<RefreshToken> tokens = refreshTokenRepository.findAll();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getIsRevoked()).isFalse();
    }

    @Test
    void 잘못된_비밀번호_로그인_거부() {
        // given
        activeUser("wrong@example.com", "Password1!");

        // when & then
        assertThatThrownBy(() -> authService.login(new LoginReq("wrong@example.com", "WrongPass1!")))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_CREDENTIALS));
    }

    // ─── 로그아웃 ─────────────────────────────────────────────────────────────

    @Test
    void 로그아웃_후_RefreshToken_revoke_처리() {
        // given
        activeUser("logout@example.com", "Password1!");
        AuthLoginResult loginResult = authService.login(new LoginReq("logout@example.com", "Password1!"));
        String refreshToken = loginResult.refreshToken();

        // when
        authService.logout(refreshToken);

        // then
        List<RefreshToken> tokens = refreshTokenRepository.findAll();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getIsRevoked()).isTrue();
    }

    // ─── 토큰 재발급 ──────────────────────────────────────────────────────────

    @Test
    void 토큰_재발급_후_기존_RT_revoke_및_새_RT_저장() {
        // given
        activeUser("refresh@example.com", "Password1!");
        AuthLoginResult loginResult = authService.login(new LoginReq("refresh@example.com", "Password1!"));
        String oldRefreshToken = loginResult.refreshToken();

        // when
        AuthLoginResult refreshResult = authService.refresh(oldRefreshToken);

        // then — 이전 토큰 revoke, 새 토큰 저장 (총 2건)
        List<RefreshToken> tokens = refreshTokenRepository.findAll();
        assertThat(tokens).hasSize(2);
        long revokedCount = tokens.stream().filter(t -> Boolean.TRUE.equals(t.getIsRevoked())).count();
        long activeCount = tokens.stream().filter(t -> !Boolean.TRUE.equals(t.getIsRevoked())).count();
        assertThat(revokedCount).isEqualTo(1);
        assertThat(activeCount).isEqualTo(1);
        assertThat(refreshResult.accessToken()).isNotEqualTo(loginResult.accessToken());
    }

    @Test
    void 이미_revoke된_토큰으로_재발급_거부() {
        // given
        activeUser("revoked@example.com", "Password1!");
        AuthLoginResult loginResult = authService.login(new LoginReq("revoked@example.com", "Password1!"));
        String rt = loginResult.refreshToken();
        authService.logout(rt); // revoke

        // when & then
        assertThatThrownBy(() -> authService.refresh(rt))
                .isInstanceOf(BusinessException.class);
    }

    // ─── 이메일 중복 확인 ─────────────────────────────────────────────────────

    @Test
    void 이메일_사용_가능_여부_조회() {
        // given
        authService.signup(new SignupReq("taken@example.com", "Password1!", "홍길동", null, UserRole.MANAGER, 1L));

        // when & then
        assertThat(authService.isEmailAvailable("taken@example.com")).isFalse();
        assertThat(authService.isEmailAvailable("free@example.com")).isTrue();
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────

    private User activeUser(String email, String rawPassword) {
        User user = User.createEmailUser(email, passwordEncoder.encode(rawPassword), "테스트유저", null, UserRole.MANAGER, 1L);
        user.approve(999L);
        return userRepository.save(user);
    }
}
