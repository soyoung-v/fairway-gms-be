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
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.CartRepository;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DesignatedCartService {

    private final CaddieRepository caddieRepository;
    private final CaddieDesignatedCartRepository designatedCartRepository;
    private final CartRepository cartRepository;
    private final GolfCourseRepository golfCourseRepository;
    private final com.fairwaygms.fairwaygmsbe.common.context.GolfCourseContextResolver contextResolver;

    // ADMIN은 X-Selected-Golf-Course-Id 헤더의 선택 골프장, MANAGER는 소속 골프장을 대상으로 한다
    private Long targetGolfCourseId(AuthenticatedUser auth) {
        return auth.isAdmin() ? contextResolver.resolveTargetGolfCourseId(auth) : auth.getGolfCourseId();
    }

    // FR-309: 골프장 전체 활성 지정카트 목록
    @Transactional(readOnly = true)
    public List<DesignatedCartRes> getDesignatedCarts(AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = targetGolfCourseId(auth);
        return designatedCartRepository.findByGolfCourse_IdAndIsActiveTrueAndIsDeletedFalse(golfCourseId)
                .stream()
                .map(DesignatedCartRes::from)
                .toList();
    }

    // FR-307: 지정카트 설정 — 기존 활성 지정카트가 있으면 먼저 비활성화
    public DesignatedCartRes setDesignatedCart(Long caddieId, SetDesignatedCartReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Caddie caddie = findCaddie(caddieId);
        validateGolfCourseAccess(caddie.getGolfCourse().getId(), auth);

        Cart cart = cartRepository.findByIdAndIsDeletedFalse(req.cartId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND));

        // 카트가 같은 골프장 소속인지 확인
        if (!cart.getGolfCourse().getId().equals(caddie.getGolfCourse().getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 이미 같은 카트가 활성 지정카트로 등록되어 있으면 그대로 반환
        var existing = designatedCartRepository.findByCaddie_IdAndIsActiveTrueAndIsDeletedFalse(caddieId);
        if (existing.isPresent()) {
            if (existing.get().getCart().getId().equals(req.cartId())) {
                return DesignatedCartRes.from(existing.get());
            }
            // 기존 활성 지정카트 비활성화
            existing.get().deactivate();
        }

        GolfCourse golfCourse = golfCourseRepository.findByIdAndIsDeletedFalse(caddie.getGolfCourse().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GOLF_COURSE_NOT_FOUND));

        CaddieDesignatedCart designatedCart = CaddieDesignatedCart.create(caddie, golfCourse, cart);
        designatedCartRepository.save(designatedCart);
        return DesignatedCartRes.from(designatedCart);
    }

    // FR-308: 지정카트 해제
    public void removeDesignatedCart(Long caddieId, AuthenticatedUser auth) {
        validateManager(auth);
        Caddie caddie = findCaddie(caddieId);
        validateGolfCourseAccess(caddie.getGolfCourse().getId(), auth);

        CaddieDesignatedCart designatedCart = designatedCartRepository
                .findByCaddie_IdAndIsActiveTrueAndIsDeletedFalse(caddieId)
                .orElseThrow(() -> new BusinessException(CaddieErrorCode.DESIGNATED_CART_NOT_FOUND));

        designatedCart.deactivate();
    }

    private Caddie findCaddie(Long caddieId) {
        return caddieRepository.findByIdAndIsDeletedFalse(caddieId)
                .orElseThrow(() -> new BusinessException(CaddieErrorCode.CADDIE_NOT_FOUND));
    }

    private void validateManager(AuthenticatedUser auth) {
        if (auth.getRole() != UserRole.MANAGER && !auth.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateGolfCourseAccess(Long caddieGolfCourseId, AuthenticatedUser auth) {
        if (auth.isAdmin()) return;
        if (!caddieGolfCourseId.equals(targetGolfCourseId(auth))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
