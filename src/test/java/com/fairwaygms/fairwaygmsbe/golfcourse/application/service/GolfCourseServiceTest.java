package com.fairwaygms.fairwaygmsbe.golfcourse.application.service;

import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.req.CreateCartReq;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.req.CreateCourseReq;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.req.CreateGolfCourseReq;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.req.UpdateCartStatusReq;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.req.UpdateGolfCourseReq;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.res.CartRes;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.res.CourseRes;
import com.fairwaygms.fairwaygmsbe.golfcourse.application.model.res.GolfCourseRes;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Cart;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Course;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.enums.CartStatus;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.enums.CartType;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.CartRepository;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.CourseRepository;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.golfcourse.exception.GolfCourseErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GolfCourseServiceTest {

    @Mock private GolfCourseRepository golfCourseRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CartRepository cartRepository;

    private GolfCourseService golfCourseService;

    @BeforeEach
    void setUp() {
        golfCourseService = new GolfCourseService(golfCourseRepository, courseRepository, cartRepository);
    }

    // ── 골프장 ─────────────────────────────────────────────────────────────────

    @Test
    void createGolfCourse_adminSucceeds() {
        // given
        CreateGolfCourseReq request = new CreateGolfCourseReq("선산CC", "경북 구미시", "054-000-0000");
        when(golfCourseRepository.save(any(GolfCourse.class))).thenAnswer(inv -> {
            GolfCourse gc = inv.getArgument(0);
            ReflectionTestUtils.setField(gc, "id", 1L);
            return gc;
        });

        // when
        GolfCourseRes response = golfCourseService.createGolfCourse(request, admin());

        // then
        ArgumentCaptor<GolfCourse> captor = ArgumentCaptor.forClass(GolfCourse.class);
        verify(golfCourseRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("선산CC");
        assertThat(response.golfCourseId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("선산CC");
    }

    @Test
    void createGolfCourse_failsWhenNotAdmin() {
        // given
        CreateGolfCourseReq request = new CreateGolfCourseReq("선산CC", null, null);

        // when & then
        assertThatThrownBy(() -> golfCourseService.createGolfCourse(request, manager(10L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void updateGolfCourse_adminSucceeds() {
        // given
        GolfCourse golfCourse = golfCourse(1L);
        when(golfCourseRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(golfCourse));
        UpdateGolfCourseReq request = new UpdateGolfCourseReq("선산GC", "경북 구미시 수정", "054-111-1111");

        // when
        GolfCourseRes response = golfCourseService.updateGolfCourse(1L, request, admin());

        // then
        assertThat(golfCourse.getName()).isEqualTo("선산GC");
        assertThat(response.name()).isEqualTo("선산GC");
    }

    @Test
    void updateGolfCourse_failsWhenNotFound() {
        // given
        when(golfCourseRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());
        UpdateGolfCourseReq request = new UpdateGolfCourseReq("선산GC", null, null);

        // when & then
        assertThatThrownBy(() -> golfCourseService.updateGolfCourse(99L, request, admin()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GOLF_COURSE_NOT_FOUND));
    }

    @Test
    void listGolfCourses_adminGetsAll() {
        // given
        when(golfCourseRepository.findAllByIsDeletedFalse())
                .thenReturn(List.of(golfCourse(1L), golfCourse(2L)));

        // when
        List<GolfCourseRes> result = golfCourseService.listGolfCourses(admin());

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    void listGolfCourses_managerGetsOwnOnly() {
        // given
        GolfCourse golfCourse = golfCourse(10L);
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(golfCourse));

        // when
        List<GolfCourseRes> result = golfCourseService.listGolfCourses(manager(10L));

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).golfCourseId()).isEqualTo(10L);
    }

    @Test
    void selectGolfCourse_managerCanSelectOwn() {
        // given
        GolfCourse golfCourse = golfCourse(10L);
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(golfCourse));

        // when
        GolfCourseRes response = golfCourseService.selectGolfCourse(10L, manager(10L));

        // then
        assertThat(response.golfCourseId()).isEqualTo(10L);
    }

    @Test
    void selectGolfCourse_managerForbiddenOnOther() {
        // when & then — Manager가 본인 소속(10L)이 아닌 다른 골프장(20L) 선택 시 FORBIDDEN
        assertThatThrownBy(() -> golfCourseService.selectGolfCourse(20L, manager(10L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    // ── 코스 ───────────────────────────────────────────────────────────────────

    @Test
    void createCourse_succeeds() {
        // given
        GolfCourse golfCourse = golfCourse(10L);
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(golfCourse));
        when(courseRepository.existsByGolfCourseAndNameAndIsDeletedFalse(golfCourse, "동코스")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 1L);
            return c;
        });
        CreateCourseReq request = new CreateCourseReq("동코스", 18, 1);

        // when
        CourseRes response = golfCourseService.createCourse(10L, request, manager(10L));

        // then
        assertThat(response.courseId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("동코스");
        assertThat(response.holeCount()).isEqualTo(18);
    }

    @Test
    void createCourse_failsOnDuplicateName() {
        // given
        GolfCourse golfCourse = golfCourse(10L);
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(golfCourse));
        when(courseRepository.existsByGolfCourseAndNameAndIsDeletedFalse(golfCourse, "동코스")).thenReturn(true);
        CreateCourseReq request = new CreateCourseReq("동코스", 18, 1);

        // when & then
        assertThatThrownBy(() -> golfCourseService.createCourse(10L, request, manager(10L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(GolfCourseErrorCode.COURSE_NAME_DUPLICATED));
    }

    @Test
    void createCourse_failsOnInvalidHoleCount() {
        // given
        CreateCourseReq request = new CreateCourseReq("동코스", 10, 1);

        // when & then — 10홀은 유효하지 않음 (9/18/27만 허용)
        assertThatThrownBy(() -> golfCourseService.createCourse(10L, request, manager(10L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(GolfCourseErrorCode.INVALID_HOLE_COUNT));
    }

    @Test
    void createCourse_managerForbiddenOnOtherGolfCourse() {
        // given
        CreateCourseReq request = new CreateCourseReq("동코스", 18, 1);

        // when & then — Manager(소속 10L)가 다른 골프장(20L)에 코스 등록 시도
        assertThatThrownBy(() -> golfCourseService.createCourse(20L, request, manager(10L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GOLF_COURSE_FORBIDDEN));
    }

    // ── 카트 ───────────────────────────────────────────────────────────────────

    @Test
    void createCart_succeeds() {
        // given
        GolfCourse golfCourse = golfCourse(10L);
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(golfCourse));
        when(cartRepository.existsByGolfCourseAndCartNumberAndIsDeletedFalse(golfCourse, "001")).thenReturn(false);
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> {
            Cart c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 1L);
            return c;
        });
        CreateCartReq request = new CreateCartReq("001", "ELECTRIC");

        // when
        CartRes response = golfCourseService.createCart(10L, request, manager(10L));

        // then
        assertThat(response.cartId()).isEqualTo(1L);
        assertThat(response.cartNumber()).isEqualTo("001");
        assertThat(response.cartType()).isEqualTo("ELECTRIC");
        assertThat(response.status()).isEqualTo("AVAILABLE");
    }

    @Test
    void createCart_failsOnDuplicateNumber() {
        // given
        GolfCourse golfCourse = golfCourse(10L);
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(golfCourse));
        when(cartRepository.existsByGolfCourseAndCartNumberAndIsDeletedFalse(golfCourse, "001")).thenReturn(true);
        CreateCartReq request = new CreateCartReq("001", "ELECTRIC");

        // when & then
        assertThatThrownBy(() -> golfCourseService.createCart(10L, request, manager(10L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(GolfCourseErrorCode.DUPLICATE_CART_NUMBER));
    }

    @Test
    void createCart_failsOnInvalidCartType() {
        // given
        CreateCartReq request = new CreateCartReq("001", "INVALID_TYPE");

        // when & then
        assertThatThrownBy(() -> golfCourseService.createCart(10L, request, manager(10L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(GolfCourseErrorCode.INVALID_STATUS));
    }

    @Test
    void updateCartStatus_failsWhenNotManager() {
        // given — Admin은 카트 상태 변경 불가 (역할 체크가 카트 조회보다 먼저 일어남)
        UpdateCartStatusReq request = new UpdateCartStatusReq("MAINTENANCE");

        // when & then
        assertThatThrownBy(() -> golfCourseService.updateCartStatus(1L, request, admin()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void updateCartStatus_succeeds() {
        // given
        Cart cart = cart(10L);
        when(cartRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(cart));
        UpdateCartStatusReq request = new UpdateCartStatusReq("MAINTENANCE");

        // when
        CartRes response = golfCourseService.updateCartStatus(1L, request, manager(10L));

        // then
        assertThat(cart.getStatus()).isEqualTo(CartStatus.MAINTENANCE);
        assertThat(response.status()).isEqualTo("MAINTENANCE");
    }

    @Test
    void returnCart_setsStatusToAvailable() {
        // given
        Cart cart = cart(10L);
        cart.changeStatus(CartStatus.MAINTENANCE);
        when(cartRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(cart));

        // when
        golfCourseService.returnCart(1L);

        // then
        assertThat(cart.getStatus()).isEqualTo(CartStatus.AVAILABLE);
    }

    @Test
    void returnCart_failsWhenCartNotFound() {
        // given
        when(cartRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> golfCourseService.returnCart(99L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(GolfCourseErrorCode.CART_NOT_FOUND));
    }

    // ── 픽스처 ─────────────────────────────────────────────────────────────────

    private AuthenticatedUser admin() {
        return new AuthenticatedUser(1L, UserRole.ADMIN, null);
    }

    private AuthenticatedUser manager(Long golfCourseId) {
        return new AuthenticatedUser(2L, UserRole.MANAGER, golfCourseId);
    }

    private GolfCourse golfCourse(Long id) {
        GolfCourse gc = GolfCourse.create("테스트CC", "테스트 주소", "000-0000-0000");
        ReflectionTestUtils.setField(gc, "id", id);
        return gc;
    }

    private Cart cart(Long golfCourseId) {
        GolfCourse gc = golfCourse(golfCourseId);
        Cart cart = Cart.create(gc, "001", CartType.ELECTRIC);
        ReflectionTestUtils.setField(cart, "id", 1L);
        return cart;
    }
}
