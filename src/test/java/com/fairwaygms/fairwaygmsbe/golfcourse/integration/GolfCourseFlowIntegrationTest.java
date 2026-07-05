package com.fairwaygms.fairwaygmsbe.golfcourse.integration;

import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.req.CreateCartReq;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.req.CreateCourseReq;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.req.CreateGolfCourseReq;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.req.UpdateCartStatusReq;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.res.CartRes;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.res.CourseRes;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.res.GolfCourseRes;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.service.GolfCourseService;
import com.fairwaygms.fairwaygmsbe.golfcourse.exception.GolfCourseErrorCode;
import com.fairwaygms.fairwaygmsbe.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GolfCourseFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired GolfCourseService golfCourseService;

    private AuthenticatedUser admin;
    private Long golfCourseId;

    @BeforeEach
    void setUp() {
        admin = new AuthenticatedUser(1L, UserRole.ADMIN, null);
        GolfCourseRes gc = golfCourseService.createGolfCourse(
                new CreateGolfCourseReq("테스트 골프장", "서울시 강남구", "02-0000-0000"), admin);
        golfCourseId = gc.golfCourseId();
    }

    // ─── 골프장 ──────────────────────────────────────────────────────────────

    @Test
    void 골프장_생성_후_목록_조회() {
        // when
        List<GolfCourseRes> list = golfCourseService.listGolfCourses(admin);

        // then — 로컬 DB에 기존 데이터가 있어도 통과하도록 생성한 골프장 포함 여부만 검증
        assertThat(list).extracting(GolfCourseRes::name).contains("테스트 골프장");

    }

    @Test
    void MANAGER는_본인_골프장만_목록_조회() {
        // given — 다른 골프장 추가
        golfCourseService.createGolfCourse(new CreateGolfCourseReq("다른 골프장", "부산시", "051-0000-0000"), admin);
        AuthenticatedUser manager = new AuthenticatedUser(99L, UserRole.MANAGER, golfCourseId);

        // when
        List<GolfCourseRes> list = golfCourseService.listGolfCourses(manager);

        // then — 본인 소속 1건만 반환
        assertThat(list).hasSize(1);
        assertThat(list.get(0).golfCourseId()).isEqualTo(golfCourseId);
    }

    @Test
    void MANAGER가_다른_골프장_선택_거부() {
        // given
        GolfCourseRes other = golfCourseService.createGolfCourse(
                new CreateGolfCourseReq("다른 골프장", "부산시", "051-0000-0000"), admin);
        AuthenticatedUser manager = new AuthenticatedUser(99L, UserRole.MANAGER, golfCourseId);

        // when & then
        assertThatThrownBy(() -> golfCourseService.selectGolfCourse(other.golfCourseId(), manager))
                .isInstanceOf(BusinessException.class);
    }

    // ─── 코스 ────────────────────────────────────────────────────────────────

    @Test
    void 코스_생성_후_목록_조회() {
        // given
        AuthenticatedUser manager = new AuthenticatedUser(99L, UserRole.MANAGER, golfCourseId);
        golfCourseService.createCourse(golfCourseId, new CreateCourseReq("A코스", 18, 1), manager);
        golfCourseService.createCourse(golfCourseId, new CreateCourseReq("B코스", 9, 2), manager);

        // when
        List<CourseRes> list = golfCourseService.listCourses(golfCourseId, manager);

        // then — sortOrder 오름차순
        assertThat(list).hasSize(2);
        assertThat(list.get(0).name()).isEqualTo("A코스");
        assertThat(list.get(1).name()).isEqualTo("B코스");
    }

    @Test
    void 동일_골프장_내_코스명_중복_거부() {
        // given
        AuthenticatedUser manager = new AuthenticatedUser(99L, UserRole.MANAGER, golfCourseId);
        golfCourseService.createCourse(golfCourseId, new CreateCourseReq("A코스", 18, 1), manager);

        // when & then
        assertThatThrownBy(() ->
                golfCourseService.createCourse(golfCourseId, new CreateCourseReq("A코스", 9, 2), manager))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(GolfCourseErrorCode.COURSE_NAME_DUPLICATED));
    }

    @Test
    void 유효하지_않은_홀수_코스_생성_거부() {
        // given — 10홀은 허용하지 않음 (9/18/27만 허용)
        AuthenticatedUser manager = new AuthenticatedUser(99L, UserRole.MANAGER, golfCourseId);

        // when & then
        assertThatThrownBy(() ->
                golfCourseService.createCourse(golfCourseId, new CreateCourseReq("C코스", 10, 1), manager))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(GolfCourseErrorCode.INVALID_HOLE_COUNT));
    }

    // ─── 카트 ────────────────────────────────────────────────────────────────

    @Test
    void 카트_생성_후_목록_조회() {
        // given
        AuthenticatedUser manager = new AuthenticatedUser(99L, UserRole.MANAGER, golfCourseId);
        golfCourseService.createCart(golfCourseId, new CreateCartReq("카트-01", "ELECTRIC"), manager);
        golfCourseService.createCart(golfCourseId, new CreateCartReq("카트-02", "MANUAL"), manager);

        // when
        List<CartRes> list = golfCourseService.listCarts(golfCourseId, null, manager);

        // then
        assertThat(list).hasSize(2);
    }

    @Test
    void 카트_번호_중복_등록_거부() {
        // given
        AuthenticatedUser manager = new AuthenticatedUser(99L, UserRole.MANAGER, golfCourseId);
        golfCourseService.createCart(golfCourseId, new CreateCartReq("카트-01", "ELECTRIC"), manager);

        // when & then
        assertThatThrownBy(() ->
                golfCourseService.createCart(golfCourseId, new CreateCartReq("카트-01", "MANUAL"), manager))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(GolfCourseErrorCode.DUPLICATE_CART_NUMBER));
    }

    @Test
    void 카트_상태_변경_후_DB_반영() {
        // given
        AuthenticatedUser manager = new AuthenticatedUser(99L, UserRole.MANAGER, golfCourseId);
        CartRes cart = golfCourseService.createCart(golfCourseId, new CreateCartReq("카트-01", "ELECTRIC"), manager);

        // when
        CartRes updated = golfCourseService.updateCartStatus(cart.cartId(), new UpdateCartStatusReq("MAINTENANCE"), manager);

        // then
        assertThat(updated.status()).isEqualTo("MAINTENANCE");
    }

    @Test
    void 카트_상태_필터링_조회() {
        // given
        AuthenticatedUser manager = new AuthenticatedUser(99L, UserRole.MANAGER, golfCourseId);
        CartRes c1 = golfCourseService.createCart(golfCourseId, new CreateCartReq("카트-01", "ELECTRIC"), manager);
        CartRes c2 = golfCourseService.createCart(golfCourseId, new CreateCartReq("카트-02", "ELECTRIC"), manager);
        golfCourseService.updateCartStatus(c1.cartId(), new UpdateCartStatusReq("MAINTENANCE"), manager);

        // when — AVAILABLE 상태만 조회
        List<CartRes> available = golfCourseService.listCarts(golfCourseId, "AVAILABLE", manager);

        // then
        assertThat(available).hasSize(1);
        assertThat(available.get(0).cartId()).isEqualTo(c2.cartId());
    }
}
