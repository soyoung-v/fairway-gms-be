package com.fairwaygms.fairwaygmsbe.assignment.application.service;

import com.fairwaygms.fairwaygmsbe.assignment.application.model.req.CartAssignReq;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.req.ChangeCartReq;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.CartAssignmentRes;
import com.fairwaygms.fairwaygmsbe.assignment.application.model.res.CartAutoAssignRes;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.Assignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.entity.CartAssignment;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.AssignmentRepository;
import com.fairwaygms.fairwaygmsbe.assignment.domain.repository.CartAssignmentRepository;
import com.fairwaygms.fairwaygmsbe.assignment.exception.AssignmentErrorCode;
import com.fairwaygms.fairwaygmsbe.caddie.domain.entity.CaddieDesignatedCart;
import com.fairwaygms.fairwaygmsbe.caddie.domain.repository.CaddieDesignatedCartRepository;
import com.fairwaygms.fairwaygmsbe.common.exception.BusinessException;
import com.fairwaygms.fairwaygmsbe.common.exception.ErrorCode;
import com.fairwaygms.fairwaygmsbe.common.security.AuthenticatedUser;
import com.fairwaygms.fairwaygmsbe.common.security.UserRole;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.Cart;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.entity.GolfCourse;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.enums.CartStatus;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.CartRepository;
import com.fairwaygms.fairwaygmsbe.golfcourse.domain.repository.GolfCourseRepository;
import com.fairwaygms.fairwaygmsbe.operation.domain.entity.TeeTime;
import com.fairwaygms.fairwaygmsbe.operation.domain.repository.TeeTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartAssignmentService {

    private final CartAssignmentRepository cartAssignmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final CaddieDesignatedCartRepository caddieDesignatedCartRepository;
    private final CartRepository cartRepository;
    private final TeeTimeRepository teeTimeRepository;
    private final GolfCourseRepository golfCourseRepository;

    // 카트-티타임 배정 — 같은 날짜+티타임에 동일 카트 중복 불가
    public CartAssignmentRes assignCart(CartAssignReq req, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = auth.getGolfCourseId();
        GolfCourse golfCourse = findGolfCourse(golfCourseId);

        Cart cart = cartRepository.findByIdAndIsDeletedFalse(req.cartId())
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.CART_NOT_FOUND));
        validateGolfCourseAccess(cart.getGolfCourse().getId(), auth);

        TeeTime teeTime = teeTimeRepository.findByIdAndIsDeletedFalse(req.teeTimeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // soft-delete 구조 보완 검증 (DB UNIQUE는 취소 행을 점유하므로 서비스 레이어에서 검증)
        if (cartAssignmentRepository.existsByCart_IdAndTeeTime_IdAndAssignmentDateAndIsDeletedFalse(
                req.cartId(), req.teeTimeId(), req.assignmentDate())) {
            throw new BusinessException(AssignmentErrorCode.CART_ALREADY_ASSIGNED);
        }

        CartAssignment cartAssignment = CartAssignment.create(golfCourse, cart, teeTime, req.assignmentDate());
        return CartAssignmentRes.from(cartAssignmentRepository.save(cartAssignment));
    }

    // 카트 반납 처리 — ASSIGNED → RETURNED 상태로 기록
    public CartAssignmentRes returnCart(Long cartAssignmentId, AuthenticatedUser auth) {
        validateManager(auth);
        CartAssignment cartAssignment = findCartAssignment(cartAssignmentId);
        validateGolfCourseAccess(cartAssignment.getGolfCourse().getId(), auth);

        cartAssignment.returnCart();
        return CartAssignmentRes.from(cartAssignment);
    }

    // 카트 배정 취소 — 소프트 삭제, 재배정 가능
    public void cancelCartAssignment(Long cartAssignmentId, AuthenticatedUser auth) {
        validateManager(auth);
        CartAssignment cartAssignment = findCartAssignment(cartAssignmentId);
        validateGolfCourseAccess(cartAssignment.getGolfCourse().getId(), auth);

        cartAssignment.cancel();
    }

    // API-520: 카트 자동 배정 — 지정카트 우선, 이후 카트 번호 순 배정
    public CartAutoAssignRes autoAssignCarts(LocalDate assignmentDate, AuthenticatedUser auth) {
        validateManager(auth);
        Long golfCourseId = auth.getGolfCourseId();
        GolfCourse golfCourse = findGolfCourse(golfCourseId);

        List<Assignment> assignments = assignmentRepository.findByGolfCourseAndDateWithDetails(golfCourseId, assignmentDate);

        // 이미 배정된 카트 현황: teeTimeId → 배정된 cartId Set
        List<CartAssignment> existingCartAssignments = cartAssignmentRepository.findByGolfCourseAndDate(golfCourseId, assignmentDate);
        Map<Long, Set<Long>> occupiedByTeeTime = existingCartAssignments.stream()
                .collect(Collectors.groupingBy(
                        ca -> ca.getTeeTime().getId(),
                        Collectors.mapping(ca -> ca.getCart().getId(), Collectors.toCollection(HashSet::new))
                ));
        Set<Long> teeTimesWithCart = existingCartAssignments.stream()
                .map(ca -> ca.getTeeTime().getId())
                .collect(Collectors.toCollection(HashSet::new));

        // 사용 가능한 카트 목록: 카트 번호 순 정렬
        List<Cart> availableCarts = cartRepository.findAllByGolfCourseAndStatusAndIsDeletedFalse(golfCourse, CartStatus.AVAILABLE)
                .stream()
                .sorted(Comparator.comparing(Cart::getCartNumber))
                .toList();

        int assignedCount = 0;
        int skippedCount = 0;

        for (Assignment assignment : assignments) {
            TeeTime teeTime = assignment.getReservationTeam().getTeeTime();
            Long teeTimeId = teeTime.getId();

            if (teeTimesWithCart.contains(teeTimeId)) {
                continue;
            }

            Set<Long> occupied = occupiedByTeeTime.computeIfAbsent(teeTimeId, k -> new HashSet<>());

            // 지정카트 우선 배정
            Cart cartToAssign = caddieDesignatedCartRepository
                    .findByCaddie_IdAndIsActiveTrueAndIsDeletedFalse(assignment.getCaddie().getId())
                    .map(CaddieDesignatedCart::getCart)
                    .filter(c -> !occupied.contains(c.getId()))
                    .orElse(null);

            // 지정카트 없거나 사용 중이면 번호 순으로 빈 카트 선택
            if (cartToAssign == null) {
                cartToAssign = availableCarts.stream()
                        .filter(c -> !occupied.contains(c.getId()))
                        .findFirst()
                        .orElse(null);
            }

            if (cartToAssign == null) {
                skippedCount++;
                continue;
            }

            cartAssignmentRepository.save(CartAssignment.create(golfCourse, cartToAssign, teeTime, assignmentDate));
            occupied.add(cartToAssign.getId());
            teeTimesWithCart.add(teeTimeId);
            assignedCount++;
        }

        return new CartAutoAssignRes(assignmentDate, assignedCount, skippedCount);
    }

    // API-521: 카트 수동 변경 — 기존 배정 취소 후 새 카트로 재배정
    public CartAssignmentRes changeCart(Long cartAssignmentId, ChangeCartReq req, AuthenticatedUser auth) {
        validateManager(auth);
        CartAssignment existing = findCartAssignment(cartAssignmentId);
        validateGolfCourseAccess(existing.getGolfCourse().getId(), auth);

        Cart newCart = cartRepository.findByIdAndIsDeletedFalse(req.newCartId())
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.CART_NOT_FOUND));
        validateGolfCourseAccess(newCart.getGolfCourse().getId(), auth);

        if (cartAssignmentRepository.existsByCart_IdAndTeeTime_IdAndAssignmentDateAndIsDeletedFalse(
                req.newCartId(), existing.getTeeTime().getId(), existing.getAssignmentDate())) {
            throw new BusinessException(AssignmentErrorCode.CART_ALREADY_ASSIGNED);
        }

        existing.cancel();
        return CartAssignmentRes.from(cartAssignmentRepository.save(
                CartAssignment.create(existing.getGolfCourse(), newCart, existing.getTeeTime(), existing.getAssignmentDate())));
    }

    // 골프장+날짜 기준 카트 배정 목록 조회
    @Transactional(readOnly = true)
    public List<CartAssignmentRes> getCartAssignments(Long golfCourseId, LocalDate date, AuthenticatedUser auth) {
        validateManager(auth);
        Long targetId = auth.isAdmin() ? golfCourseId : auth.getGolfCourseId();
        return cartAssignmentRepository.findByGolfCourseAndDate(targetId, date)
                .stream()
                .map(CartAssignmentRes::from)
                .toList();
    }

    private CartAssignment findCartAssignment(Long id) {
        return cartAssignmentRepository.findById(id)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new BusinessException(AssignmentErrorCode.CART_ASSIGNMENT_NOT_FOUND));
    }

    private GolfCourse findGolfCourse(Long golfCourseId) {
        return golfCourseRepository.findByIdAndIsDeletedFalse(golfCourseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GOLF_COURSE_NOT_FOUND));
    }

    private void validateManager(AuthenticatedUser auth) {
        if (auth.getRole() != UserRole.MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateGolfCourseAccess(Long resourceGolfCourseId, AuthenticatedUser auth) {
        if (auth.isAdmin()) return;
        if (!resourceGolfCourseId.equals(auth.getGolfCourseId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
