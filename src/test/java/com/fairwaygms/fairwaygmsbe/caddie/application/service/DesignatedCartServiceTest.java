package com.fairwaygms.fairwaygmsbe.caddie.application.service;

import com.fairwaygms.fairwaygmsbe.caddie.application.model.req.SetDesignatedCartReq;
import com.fairwaygms.fairwaygmsbe.caddie.application.model.res.DesignatedCartRes;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.Caddie;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieDesignatedCart;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieDesignatedCartRepository;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieRepository;
import com.fairwaygms.fairwaygmsbe.caddie.exception.CaddieErrorCode;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Cart;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.enums.CartType;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.CartRepository;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DesignatedCartServiceTest {

    @Mock private CaddieRepository caddieRepository;
    @Mock private CaddieDesignatedCartRepository designatedCartRepository;
    @Mock private CartRepository cartRepository;
    @Mock private GolfCourseRepository golfCourseRepository;
    @Mock private com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver contextResolver;

    private DesignatedCartService designatedCartService;

    @BeforeEach
    void setUp() {
        designatedCartService = new DesignatedCartService(
                caddieRepository, designatedCartRepository, cartRepository, golfCourseRepository, contextResolver
        );
    }

    // ─── 지정카트 설정 ────────────────────────────────────────────────

    @Test
    void 지정카트_설정_기존_활성_카트_비활성화_후_신규_등록() {
        // given
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = caddie(1L, gc);
        Cart oldCart = cart(100L, gc);
        Cart newCart = cart(200L, gc);

        CaddieDesignatedCart existing = CaddieDesignatedCart.create(caddie, gc, oldCart);

        when(caddieRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(caddie));
        when(cartRepository.findByIdAndIsDeletedFalse(200L)).thenReturn(Optional.of(newCart));
        when(designatedCartRepository.findByCaddie_IdAndIsActiveTrueAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(existing));
        when(golfCourseRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(gc));
        when(designatedCartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        designatedCartService.setDesignatedCart(1L, new SetDesignatedCartReq(200L), manager(10L));

        // then — 기존 지정카트가 비활성화되어야 함
        assertThat(existing.getIsActive()).isFalse();
        assertThat(existing.getIsDeleted()).isTrue();
    }

    @Test
    void 같은_카트_재설정시_기존_반환() {
        // given
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = caddie(1L, gc);
        Cart cart = cart(100L, gc);
        CaddieDesignatedCart existing = CaddieDesignatedCart.create(caddie, gc, cart);
        ReflectionTestUtils.setField(cart, "id", 100L);

        when(caddieRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(caddie));
        when(cartRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(cart));
        when(designatedCartRepository.findByCaddie_IdAndIsActiveTrueAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(existing));

        // when
        designatedCartService.setDesignatedCart(1L, new SetDesignatedCartReq(100L), manager(10L));

        // then — 동일 카트이므로 새 엔티티 저장 없음
        verify(designatedCartRepository, never()).save(any());
        assertThat(existing.getIsActive()).isTrue();
    }

    @Test
    void 다른_골프장_카트_설정시_FORBIDDEN() {
        // given — 카트가 다른 골프장 소속
        GolfCourse gc = golfCourse(10L);
        GolfCourse otherGc = golfCourse(99L);
        Caddie caddie = caddie(1L, gc);
        Cart cart = cart(200L, otherGc);

        when(caddieRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(caddie));
        when(cartRepository.findByIdAndIsDeletedFalse(200L)).thenReturn(Optional.of(cart));

        // when & then
        assertThatThrownBy(() -> designatedCartService.setDesignatedCart(1L, new SetDesignatedCartReq(200L), manager(10L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    // ─── 지정카트 해제 ────────────────────────────────────────────────

    @Test
    void 지정카트_해제_정상() {
        // given
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = caddie(1L, gc);
        Cart cart = cart(100L, gc);
        CaddieDesignatedCart existing = CaddieDesignatedCart.create(caddie, gc, cart);

        when(caddieRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(caddie));
        when(designatedCartRepository.findByCaddie_IdAndIsActiveTrueAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(existing));

        // when
        designatedCartService.removeDesignatedCart(1L, manager(10L));

        // then
        assertThat(existing.getIsActive()).isFalse();
        assertThat(existing.getIsDeleted()).isTrue();
    }

    @Test
    void 활성_지정카트_없을_때_해제시_예외() {
        // given
        GolfCourse gc = golfCourse(10L);
        Caddie caddie = caddie(1L, gc);

        when(caddieRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(caddie));
        when(designatedCartRepository.findByCaddie_IdAndIsActiveTrueAndIsDeletedFalse(1L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> designatedCartService.removeDesignatedCart(1L, manager(10L)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CaddieErrorCode.DESIGNATED_CART_NOT_FOUND));
    }

    // ─── 헬퍼 ───────────────────────────────────────────────────────

    private AuthenticatedUser manager(Long golfCourseId) {
        return new AuthenticatedUser(10L, UserRole.MANAGER, golfCourseId);
    }

    private GolfCourse golfCourse(Long id) {
        GolfCourse gc = GolfCourse.create("테스트 골프장", "주소", "010-0000-0000");
        ReflectionTestUtils.setField(gc, "id", id);
        return gc;
    }

    private Caddie caddie(Long id, GolfCourse gc) {
        Caddie c = Caddie.createOnApproval(gc, null, "테스트 캐디");
        c.updateInfo("A01", null, null);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private Cart cart(Long id, GolfCourse gc) {
        Cart c = Cart.create(gc, "C-" + id, CartType.ELECTRIC);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
